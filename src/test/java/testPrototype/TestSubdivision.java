package testPrototype;

import advancedGeometry.ZSkeleton;
import advancedGeometry.subdivision.ZSD_SkeVorStrip;
import advancedGeometry.subdivision.ZSD_Voronoi;
import basicGeometry.ZPoint;
import guo_cam.CameraController;
import igeo.ICurve;
import igeo.IG;
import math.ZGeoMath;
import org.locationtech.jts.geom.Polygon;
import processing.core.PApplet;
import render.JtsRender;
import render.ZRender;
import transform.ZTransform;
import wblut.geom.*;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * description
 *
 * @author ZHANG Baizhou zhangbz
 * @project shopping_mall
 * @date 2022/6/1
 * @time 12:04
 */
public class TestSubdivision extends PApplet {

    public static void main(String[] args) {
        PApplet.main("testPrototype.TestSubdivision");
    }

    /* ------------- settings ------------- */

    public void settings() {
        size(1000, 1000, P3D);
    }

    /* ------------- setup ------------- */

    private Polygon b1;
    private Polygon offset;
    private List<ZPoint> divPts;
    private List<WB_Polygon> vors;
    private Polygon b2;
    private ZSkeleton skeleton;

    private CameraController gcam;
    private JtsRender jtsRender;
    private WB_Render render;

    public void setup() {
        gcam = new CameraController(this);
        jtsRender = new JtsRender(this);
        gcam.top();
        render = new WB_Render(this);

        String path = ".\\src\\test\\resources\\testSub.3dm";
        IG.init();
        IG.open(path);

        ICurve[] bo1 = IG.layer("b1").curves();
        this.b1 = (Polygon) ZTransform.ICurveToJts(bo1[0]);
        ICurve[] bo2 = IG.layer("b2").curves();
        this.b2 = (Polygon) ZTransform.ICurveToJts(bo2[0]);


        this.offset = (Polygon) b1.buffer(-8);
        this.divPts = ZGeoMath.splitPolygonEdge(offset, 10);
        WB_Point[] generator = new WB_Point[divPts.size()];
        for (int i = 0; i < divPts.size(); i++) {
            generator[i] = divPts.get(i).toWB_Point();
        }
        WB_Voronoi2D voronoi = WB_VoronoiCreator.getClippedVoronoi2D(generator, ZTransform.PolygonToWB_Polygon(b1));
        this.vors = new ArrayList<>();
        for (WB_VoronoiCell2D cell2D : voronoi.getCells()) {
            vors.add(cell2D.getPolygon());
        }


        this.skeleton = new ZSkeleton(b2);
    }

    /* ------------- draw ------------- */

    public void draw() {
        background(255);
        jtsRender.drawGeometry(b1);
        for (ZPoint p : divPts) {
            ZRender.drawZPoint2D(this, p, 5);
        }
        for (WB_Polygon cell : vors) {
            render.drawPolygonEdges(cell);
        }
        jtsRender.drawGeometry(offset);

        jtsRender.drawGeometry(b2);
        skeleton.display(this);

    }

}
