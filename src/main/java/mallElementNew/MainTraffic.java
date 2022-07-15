package mallElementNew;

import advancedGeometry.ZBSpline;
import advancedGeometry.ZSkeleton;
import basicGeometry.*;
import math.ZGeoMath;
import math.ZGraphMath;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import transform.ZTransform;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Point;
import wblut.geom.WB_PolyLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description
 *
 * @author zhangbz ZHANG Baizhou
 * @project shopping_mall
 * @date 2021/9/10
 * @time 17:44
 */
public class MainTraffic {
    private LineString mainTrafficCurve;           // 主路径轴线（共用）
    private Polygon mainTrafficBuffer;              // 主路径区域（共用）

    private WB_Point[] allControlNodes;
    private List<WB_Point> innerNodes;       // 内部控制点（共用）
    private List<WB_Point> entryNodes;       // 轮廓控制点（共用）

    private double trafficLength;

    /* ------------- constructor ------------- */

    public MainTraffic(Polygon boundary, double bufferDist) {
        initTraffic(boundary, bufferDist);
    }

    /* ------------- member function ------------- */

    /**
     * initialize main traffic
     *
     * @param boundary   boundary polygon
     * @param bufferDist distance to buffer
     * @return void
     */
    public void initTraffic(Polygon boundary, double bufferDist) {
        // find ridges of skeleton
        ZSkeleton boundarySkel = new ZSkeleton(boundary);
        List<ZLine> centralSegs = boundarySkel.getRidges();
//        centralSegs.addAll(boundarySkel.getExtendedRidges());

        ZGraph ridgeGraph = ZFactory.createZGraphFromSegments(centralSegs);
        ridgeGraph.checkPath();
        ridgeGraph.checkLoop();

        // check if the ridge graph has loops or is a path
        LineString centralLs = null;
        if (!ridgeGraph.isLoop()) {
            if (ridgeGraph.isPath()) {
                // single path
                centralLs = ZFactory.createLineString(centralSegs);
            } else {
                // has forks, find the longest chain
                List<ZEdge> longestChain = ZGraphMath.longestChain(ridgeGraph);
                centralLs = ZFactory.createLineString(longestChain);
            }
        }

        // divide and add entries
        assert centralLs != null;
        List<ZPoint> dividePts = ZGeoMath.splitPolyLineEdge(centralLs, 6);
//        dividePts.remove(0);
//        dividePts.remove(dividePts.size() - 1);
        this.allControlNodes = new WB_Point[dividePts.size() + 2];
        for (int i = 0; i < dividePts.size(); i++) {
            allControlNodes[i + 1] = dividePts.get(i).toWB_Point();
        }
        WB_PolyLine boundLS = ZTransform.PolygonToWB_PolyLine(boundary).get(0);
        WB_Point entryP1 = WB_GeometryOp.getClosestPoint2D(dividePts.get(0).toWB_Point(), boundLS);
        WB_Point entryP2 = WB_GeometryOp.getClosestPoint2D(dividePts.get(dividePts.size() - 1).toWB_Point(), boundLS);
        allControlNodes[0] = entryP1;
        allControlNodes[allControlNodes.length - 1] = entryP2;

        // build traffic curve, and make buffer polygon
        this.mainTrafficCurve = new ZBSpline(allControlNodes, 3, 50, ZBSpline.CLAMPED).getAsLineString();
        this.mainTrafficBuffer = (Polygon) mainTrafficCurve.buffer(bufferDist);
        this.trafficLength = mainTrafficCurve.getLength();

        this.innerNodes = new ArrayList<>();
        innerNodes.addAll(Arrays.asList(allControlNodes).subList(1, allControlNodes.length - 1));
        this.entryNodes = new ArrayList<>();
        entryNodes.add(allControlNodes[0]);
        entryNodes.add(allControlNodes[allControlNodes.length - 1]);
    }

    /**
     * update main traffic
     *
     * @param entryNodes_receive received entry node
     * @param innerNodes_receive received inner node
     * @param bufferDist         distance to buffer
     * @return void
     */
    public void updateTraffic(List<WB_Point> entryNodes_receive, List<WB_Point> innerNodes_receive, double bufferDist) {
        this.innerNodes = innerNodes_receive;
        this.entryNodes = entryNodes_receive;

        this.allControlNodes = new WB_Point[innerNodes_receive.size() + entryNodes_receive.size()];
        allControlNodes[0] = entryNodes_receive.get(0);
        for (int i = 0; i < innerNodes_receive.size(); i++) {
            allControlNodes[i + 1] = innerNodes_receive.get(i);
        }
        allControlNodes[allControlNodes.length - 1] = entryNodes_receive.get(entryNodes_receive.size() - 1);

        // build traffic curve, and make buffer polygon
        this.mainTrafficCurve = new ZBSpline(allControlNodes, 3, 50, ZBSpline.CLAMPED).getAsLineString();
        this.mainTrafficBuffer = (Polygon) mainTrafficCurve.buffer(bufferDist);
        this.trafficLength = mainTrafficCurve.getLength();
    }

    /**
     * update main traffic width
     *
     * @param bufferDist distance to buffer
     * @return void
     */
    public void updateTrafficWidth(double bufferDist) {
        // build traffic curve, and make buffer polygon
        this.mainTrafficCurve = new ZBSpline(allControlNodes, 3, 50, ZBSpline.CLAMPED).getAsLineString();
        this.mainTrafficBuffer = (Polygon) mainTrafficCurve.buffer(bufferDist);
        this.trafficLength = mainTrafficCurve.getLength();
    }
    /* ------------- setter & getter ------------- */

    public LineString getMainTrafficCurve() {
        return mainTrafficCurve;
    }

    public WB_PolyLine getMainTrafficCurveWB(){
        return ZTransform.LineStringToWB_PolyLine(mainTrafficCurve);
    }

    public LineString getMainTrafficInnerLS() {
        return new ZBSpline(innerNodes.toArray(new WB_Point[0]), 3, 50, ZBSpline.CLAMPED).getAsLineString();
    }

    public WB_PolyLine getMainTrafficInnerWB() {
        return new ZBSpline(innerNodes.toArray(new WB_Point[0]), 3, 50, ZBSpline.CLAMPED).getAsWB_PolyLine();
    }

    public Polygon getMainTrafficBuffer() {
        return mainTrafficBuffer;
    }

    public double getTrafficLength() {
        return trafficLength;
    }

    public List<WB_Point> getInnerNodes() {
        return innerNodes;
    }

    public List<WB_Point> getEntryNodes() {
        return entryNodes;
    }

}
