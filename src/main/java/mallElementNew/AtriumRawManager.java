package mallElementNew;

import basicGeometry.ZFactory;
import basicGeometry.ZPoint;
import math.ZGeoMath;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import wblut.geom.WB_GeometryOp2D;
import wblut.geom.WB_Point;
import wblut.geom.WB_PolyLine;

import java.util.ArrayList;
import java.util.List;

/**
 * the manager and creator of raw atriums
 *
 * @author zhangbz ZHANG Baizhou
 * @project shopping_mall
 * @date 2021/8/25
 * @time 16:09
 */
public class AtriumRawManager {
    private WB_PolyLine trafficLine;
    private double trafficLength;

    private List<AtriumRaw> atriumRaws;
    private List<Double> posPercentage;

    private boolean validAtriumRaw = true;

    /* ------------- constructor ------------- */

    public AtriumRawManager(WB_PolyLine mainTrafficCurve) {
        this.trafficLine = mainTrafficCurve;
        this.trafficLength = ZGeoMath.getPolyLength(trafficLine);

        this.posPercentage = new ArrayList<>();
        this.atriumRaws = new ArrayList<>();
    }

    /* ------------- member function ------------- */

    /**
     * add an AtriumRaw to the manager, sort by position, and set anchor etc.
     *
     * @param atriumRaw new AtriumRaw
     * @return void
     */
    public void addAtriumRaw(AtriumRaw atriumRaw) {
        WB_Point center = atriumRaw.getCenter();
        WB_Point closestAnchor = WB_GeometryOp2D.getClosestPoint2D(center, trafficLine);
        double dist = ZGeoMath.distFromStart(trafficLine, closestAnchor);
        double p = dist / trafficLength;

        if (posPercentage.size() == 0) {
            posPercentage.add(p);
            atriumRaw.setPercentTraffic(p);
            ZPoint vec = new ZPoint(atriumRaw.getCenter()).sub(new ZPoint(closestAnchor));
            atriumRaw.setVecFromAnchor(vec);
            ZPoint[] ptsBesidesAnchor = ZGeoMath.pointOnEdgeByDist(new ZPoint(closestAnchor), trafficLine, 0.1);
            ZPoint anchorTangent = ptsBesidesAnchor[0].sub(ptsBesidesAnchor[1]).normalize();
            atriumRaw.setAnchorTangent(anchorTangent);
            atriumRaws.add(atriumRaw);
        } else {
            if (p >= posPercentage.get(posPercentage.size() - 1)) {
                posPercentage.add(p);
                atriumRaw.setPercentTraffic(p);
                ZPoint vec = new ZPoint(atriumRaw.getCenter()).sub(new ZPoint(closestAnchor));
                atriumRaw.setVecFromAnchor(vec);
                ZPoint[] ptsBesidesAnchor = ZGeoMath.pointOnEdgeByDist(new ZPoint(closestAnchor), trafficLine, 0.1);
                ZPoint anchorTangent = ptsBesidesAnchor[0].sub(ptsBesidesAnchor[1]).normalize();
                atriumRaw.setAnchorTangent(anchorTangent);
                atriumRaws.add(atriumRaw);
            } else {
                for (int i = 0; i < posPercentage.size(); i++) {
                    if (p < posPercentage.get(i)) {
                        posPercentage.add(i, p);
                        atriumRaw.setPercentTraffic(p);
                        ZPoint vec = new ZPoint(atriumRaw.getCenter()).sub(new ZPoint(closestAnchor));
                        atriumRaw.setVecFromAnchor(vec);
                        ZPoint[] ptsBesidesAnchor = ZGeoMath.pointOnEdgeByDist(new ZPoint(closestAnchor), trafficLine, 0.1);
                        ZPoint anchorTangent = ptsBesidesAnchor[0].sub(ptsBesidesAnchor[1]).normalize();
                        atriumRaw.setAnchorTangent(anchorTangent);
                        atriumRaws.add(i, atriumRaw);
                        break;
                    }
                }
            }
        }

        validateAtriumRaw();
    }

    /**
     * remove an AtriumRaw from the manager
     *
     * @param id index of AtriumRaw
     * @return void
     */
    public void removeAtriumRaw(int id) {
        atriumRaws.remove(id);
        posPercentage.remove(id);
    }

    /**
     * change curve shape or polygon shape of the selected AtriumRaw
     *
     * @param id index of AtriumRaw
     * @return void
     */
    public void switchAtriumRawCurve(int id) {
        AtriumRaw raw = atriumRaws.get(id);
        raw.reverseCurve();
        validateAtriumRaw();
    }

    /**
     * rotate AtriumRaw
     *
     * @param id    index of AtriumRaw
     * @param angle angle to rotate
     * @return void
     */
    public void rotateAtriumRaw(int id, double angle) {
        AtriumRaw raw = atriumRaws.get(id);
        raw.rotateByAngle(angle);
        validateAtriumRaw();
    }

    /**
     * change AtriumRaw area
     *
     * @param id   index of AtriumRaw
     * @param area new area to change
     * @return void
     */
    public void changeAtriumRawArea(int id, double area) {
        AtriumRaw raw = atriumRaws.get(id);
        raw.scaleShapeByArea(area);
        validateAtriumRaw();
    }

    /**
     * update the position of AtriumRaws by a new traffic line
     *
     * @param newCenterLine new traffic center line
     * @return void
     */
    public void updateAtriumRawByTraffic(WB_PolyLine newCenterLine) {
        this.trafficLine = newCenterLine;
        this.trafficLength = ZGeoMath.getPolyLength(trafficLine);
        for (int i = 0; i < atriumRaws.size(); i++) {
            AtriumRaw a = atriumRaws.get(i);
            double pct = a.getPercentTraffic();

            double dist = trafficLength * pct;
            WB_Point newAnchor = ZGeoMath.getPointOnPolyEdge(trafficLine, dist);
            ZPoint[] ptsBesidesAnchor = ZGeoMath.pointOnEdgeByDist(new ZPoint(newAnchor), trafficLine, 0.1);
            ZPoint newAnchorTangent = ptsBesidesAnchor[0].sub(ptsBesidesAnchor[1]).normalize();
            double angle = a.getAnchorTangent().angleWith(newAnchorTangent);
            ZPoint newVecFromAnchor = a.getVecFromAnchor().rotate2D(Math.PI * (angle / 180));

            a.setAnchorTangent(newAnchorTangent);
            a.setVecFromAnchor(newVecFromAnchor);
            WB_Point newCenter = newAnchor.add(newVecFromAnchor.toWB_Point());
            a.moveByCenter(newCenter);
            a.rotateByAngle(Math.PI * (angle / 180));
        }
        validateAtriumRaw();
    }

    /**
     * validate if any raw atriums are overlap
     *
     * @return void
     */
    public void validateAtriumRaw() {
        if (atriumRaws == null || atriumRaws.size() < 2) {
            this.validAtriumRaw = true;
        } else {
            boolean flag = true;

            List<Geometry> geometryList;
            Geometry[] geometries;
            GeometryCollection gc;

            geometryList = new ArrayList<>();
            geometryList.add(atriumRaws.get(0).getShape());

            for (int i = 1; i < atriumRaws.size(); i++) {
                geometries = geometryList.toArray(new Geometry[0]);
                gc = ZFactory.jtsgf.createGeometryCollection(geometries);

                Polygon shape = atriumRaws.get(i).getShape();
                if (gc.intersects(shape)) {
                    flag = false;
                    break;
                } else {
                    geometryList.add(shape);
                }
            }

            this.validAtriumRaw = flag;
        }
    }

    /* ------------- setter & getter ------------- */

    public List<AtriumRaw> getAtriumRaws() {
        return atriumRaws;
    }

    public int getNumAtriumRaw() {
        return atriumRaws.size();
    }

    public boolean getValidAtriumRaw() {
        return validAtriumRaw;
    }

}
