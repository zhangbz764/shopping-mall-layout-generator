package testPrototype;

import advancedGeometry.rectCover.ZRectCover;
import basicGeometry.ZLine;
import basicGeometry.ZPoint;
import guo_cam.CameraController;
import igeo.ICurve;
import igeo.IG;
import mallElementNew.AuxiliarySpace;
import mallElementNew.StructureGrid;
import math.ZGeoMath;
import org.locationtech.jts.geom.Polygon;
import processing.core.PApplet;
import render.ZRender;
import transform.ZTransform;
import wblut.geom.WB_PolyLine;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * description
 *
 * @author ZHANG Baizhou zhangbz
 * @project shopping_mall
 * @date 2022/4/4
 * @time 16:18
 */
public class TestMallEvac extends PApplet {

    /* ------------- settings ------------- */

    public void settings() {
        size(1000, 1000, P3D);
    }

    /* ------------- setup ------------- */

    private List<WB_Polygon> shops;
    private WB_PolyLine traffic;
    private WB_Polygon boundary;
    private List<Polygon> atriums;

    private StructureGrid[] grids;

    private AuxiliarySpace auxiliarySpace;
    private List<ZPoint> evacPos;
    private List<ZLine> coveredPath;

    private ZPoint[][] dirTest;

    private WB_Render render;
    private CameraController gcam;

    public void setup() {
        this.render = new WB_Render(this);
        this.gcam = new CameraController(this);
        gcam.top();

        load();

        // structure grids
        ZRectCover z = new ZRectCover(boundary, 2);
        this.grids = new StructureGrid[z.getBestRects().size()];
        for (int i = 0; i < grids.length; i++) {
            grids[i] = new StructureGrid(z.getBestRects().get(i), 8.4);
        }

        // auxiliary space
        this.auxiliarySpace = new AuxiliarySpace();

        double area = Math.abs(boundary.getSignedArea());
//        for (Polygon p : atriums) {
//            area -= p.getArea();
//        }
        auxiliarySpace.initEvacuationGenerator(
                traffic,
                shops,
                area,
                boundary
        );

        this.evacPos = auxiliarySpace.getSelGeneratorPos();
        System.out.println(evacPos);
        this.coveredPath = auxiliarySpace.getCoveredPath();

        // stairway shape
//        auxiliarySpace.generateStairwayShape(boundary, grids);
    }

    private void load() {
        String path = ".\\src\\test\\resources\\testEvac.3dm";
        IG.init();
        IG.open(path);

        ICurve[] trafficCurve = IG.layer("traffic").curves();
        this.traffic = ZTransform.ICurveToWB_PolyLine(trafficCurve[0]);

        ICurve[] shopPoly = IG.layer("shop").curves();
        this.shops = new ArrayList<>();
        for (ICurve iCurve : shopPoly) {
            WB_Polygon polygon = ZGeoMath.polygonFaceUp((WB_Polygon) ZTransform.ICurveToWB(iCurve));
            ZTransform.validateWB_Polygon(polygon);
            shops.add(polygon);
        }

        ICurve[] boundaryCurve = IG.layer("boundary").curves();
        this.boundary = (WB_Polygon) ZTransform.ICurveToWB(boundaryCurve[0]);

        ICurve[] atriumPoly = IG.layer("atrium").curves();
        this.atriums = new ArrayList<>();
        for (ICurve iCurve : atriumPoly) {
            Polygon polygon = (Polygon) ZTransform.ICurveToJts(iCurve);
            atriums.add(polygon);
        }
    }

    /* ------------- draw ------------- */

    public void draw() {
        background(255);

        stroke(0);
        strokeWeight(1);
        for (WB_Polygon p : shops) {
            render.drawPolygonEdges2D(p);
        }
        render.drawPolylineEdges(traffic);


        stroke(255, 0, 0);
        strokeWeight(3);
        for (ZLine l : coveredPath) {
            ZRender.drawZLine2D(this, l);
        }

        for (ZPoint p : evacPos) {
            ZRender.drawZPoint(this, p, 5);
        }

//        stroke(0, 0, 255);
//        for (ZPoint p : auxiliarySpace.getDividePts()) {
//            ZRender.drawZPoint(this, p, 5);
//        }

        stroke(0, 255, 0);
        for (int i = 0; i < evacPos.size(); i++) {
            ZPoint[] pair = dirTest[i];
            for (ZPoint dir : pair) {
                ZRender.drawZPointAsVec(this, dir, evacPos.get(i), 10, 1);
            }
        }
    }

}
