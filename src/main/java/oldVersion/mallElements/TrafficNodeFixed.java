package oldVersion.mallElements;

import basicGeometry.ZPoint;
import math.ZGeoMath;
import processing.core.PApplet;
import render.ZRender;
import transform.ZTransform;
import wblut.geom.WB_GeometryOp2D;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * the control node on the boundary (represents entries)
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/25
 * @time 13:34
 */
public class TrafficNodeFixed extends TrafficNode {
    private final WB_Polygon boundary;
    private List<ZPoint> joints;  // join points on boundary

    /* ------------- constructor ------------- */

    public TrafficNodeFixed(double x, double y, WB_Polygon boundary) {
        super(x, y);
        this.boundary = boundary;
    }

    public TrafficNodeFixed(WB_Point p, WB_Polygon boundary) {
        super(p);
        this.boundary = boundary;
    }

    /* ------------- set & get ------------- */

    /**
     * @return void
     * @description set location to the closest point on boundary polygon
     */
    @Override
    public void setByRestriction(double pointerX, double pointerY) {
        WB_Point point = new WB_Point(pointerX, pointerY);
        this.set(WB_GeometryOp2D.getClosestPoint2D(point, ZTransform.WB_PolygonToWB_PolyLine(boundary).get(0)));
    }

    /**
     * @return void
     * @description set join points on each bisector, distance = regionR
     */
    @Override
    public void setJoints() {
        this.joints = new ArrayList<>();
        ZPoint[] besides = ZGeoMath.pointOnEdgeByDist(this, boundary, getRegionR());
        joints.add(besides[1]);
        joints.add(besides[0]);
    }

    @Override
    public void setAtrium() {
    }

    @Override
    public void clearAtrium() {

    }

    @Override
    public List<ZPoint> getJoints() {
        return this.joints;
    }

    @Override
    public String getNodeType() {
        return "TrafficNodeFixed";
    }

    @Override
    public AtriumOld getAtrium() {
        throw new NullPointerException("Fixed node can't have atrium");
    }

    @Override
    public boolean hasAtrium() {
        return false;
    }

    @Override
    public boolean isAtriumActive() {
        return false;
    }

    /* ------------- draw -------------*/

    @Override
    public void displayJoint(PApplet app, float r) {
        if (joints != null && joints.size() != 0) {
            for (ZPoint joint : joints) {
                ZRender.drawZPoint2D(app, joint, r);
//                app.line((float) this.x(), (float) this.y(), (float) joint.x(), (float) joint.y());
            }
        }
    }
}
