package oldVersion.mallElements;

import basicGeometry.ZFactory;
import basicGeometry.ZLine;
import basicGeometry.ZPoint;
import math.ZGeoMath;
import oldVersion.MallConstant;
import processing.core.PApplet;
import render.ZRender;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * atrium in the public space of a shopping mall
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/12
 * @time 10:00
 */

public class AtriumOld {
    // center node
    private TrafficNode center;

    // boundary points by computing
    private List<ZPoint> boundaryPoints;
    private List<ZPoint> vecFromCenter;

    // shape polygon
    private WB_Polygon polygon;

    // offset line
    private List<ZLine> mainSegmentsOfAtrium;
    private List<ZLine> offsetSegmentsFromAtrium;
    private List<ZPoint> jointsFromAtrium;
    private List<ControlPoint> controlPoints;
    private int activeIndex = 0;

    private static double corridorWidth = MallConstant.ATRIUM_CORRIDOR_WIDTH * MallConstant.SCALE;
    private double distFromEdge = MallConstant.ATRIUM_WIDTH * 0.5 * MallConstant.SCALE;

    // escalator
    private Escalator escalator;

    /* ------------- constructor ------------- */

    public AtriumOld(TrafficNode center) {
        this.center = center;
        initAtrium();
    }

    /* ------------- initializer ------------- */

    /**
     * main initializer of an atrium
     *
     * @return void
     */
    public void initAtrium() {
        initControlPoints(center);
        extractBoundaryPoints();
        createPolygon(vecFromCenter, boundaryPoints);
        findOffsetAndJoints(polygon, center.getLinkedEdges());
    }

    /* ------------- member function ------------- */

    /**
     * find control point for adjustment
     *
     * @param center input TrafficNode
     * @return void
     */
    private void initControlPoints(TrafficNode center) {
        this.controlPoints = new ArrayList<>();
        if (center.isEnd()) {
            // end of a graph
            ControlPoint pt1 = new ControlPoint(center, center.getVecNorToNeighbor(0), center.getLinkedEdge(0).getLength() * 0.35);
            ControlPoint pt2 = new ControlPoint(center, center.getVecNorToNeighbor(0).scaleTo(-1), 0);
            controlPoints.add(pt1);
            controlPoints.add(pt2);
        } else if (center.getLinkedEdgeNum() == 2) {
            // on edge
            for (int i = 0; i < center.getNeighborNum(); i++) {
                ControlPoint pt = new ControlPoint(center, center.getVecNorToNeighbor(i), center.getLinkedEdge(i).getLength() * 0.35);
                controlPoints.add(pt);
            }
            // at convex point
            ZPoint[] sortedVec = ZGeoMath.sortPolarAngle(center.getVecNorToNeighbors());
            for (int i = 0; i < sortedVec.length; i++) {
                if (sortedVec[i].cross2D(sortedVec[(i + 1) % sortedVec.length]) < 0) {
                    ZPoint bisector = ZGeoMath.getAngleBisectorOrdered(sortedVec[i], sortedVec[(i + 1) % sortedVec.length]);
                    ControlPoint pt = new ControlPoint(center, bisector, sortedVec[i]);
                    controlPoints.add(pt);
                }
            }
        } else if (center.getLinkedEdgeNum() > 2) {
            // on edge
            for (int i = 0; i < center.getNeighborNum(); i++) {
                ControlPoint pt = new ControlPoint(center, center.getVecNorToNeighbor(i), center.getLinkedEdge(i).getLength() * 0.3);
                controlPoints.add(pt);
            }
            // at convex point
            ZPoint[] sortedVec = ZGeoMath.sortPolarAngle(center.getVecNorToNeighbors());
            for (int i = 0; i < sortedVec.length; i++) {
                if (sortedVec[i].cross2D(sortedVec[(i + 1) % sortedVec.length]) < 0) {
                    ZPoint bisector = ZGeoMath.getAngleBisectorOrdered(sortedVec[i], sortedVec[(i + 1) % sortedVec.length]);
                    ControlPoint pt = new ControlPoint(center, bisector, sortedVec[i]);
                    controlPoints.add(pt);
                }
            }
        }
    }

    /**
     * extract boundary points from control points
     *
     * @return void
     */
    private void extractBoundaryPoints() {
        // extract boundary points from control points
        this.boundaryPoints = new ArrayList<>();
        this.vecFromCenter = new ArrayList<>();
        for (ControlPoint cp : controlPoints) {
            for (ZPoint p : cp.polyPoints) {
                boundaryPoints.add(p);
                vecFromCenter.add(p.sub(center));
            }
        }
    }

    /**
     * sort vectors and create atrium shape polygon
     *
     * @param vecFromCenter  vectors to sort
     * @param boundaryPoints all points
     * @return void
     */
    private void createPolygon(List<ZPoint> vecFromCenter, List<ZPoint> boundaryPoints) {
        // sort polar angle to order polygon vertices
        int[] order = ZGeoMath.sortPolarAngleIndices(vecFromCenter);
        WB_Point[] polyPoints = new WB_Point[boundaryPoints.size() + 1];
        for (int i = 0; i < boundaryPoints.size(); i++) {
            polyPoints[i] = boundaryPoints.get(order[i]).toWB_Point();
        }
        polyPoints[polyPoints.length - 1] = polyPoints[0];
        this.polygon = ZFactory.wbgf.createSimplePolygon(polyPoints);
    }

    /**
     * record offset segment as block boundary
     *
     * @param polygon   input polygon
     * @param testLines lines to check intersection
     * @return void
     */
    private void findOffsetAndJoints(WB_Polygon polygon, List<? extends ZLine> testLines) {
        this.mainSegmentsOfAtrium = new ArrayList<>();
        this.offsetSegmentsFromAtrium = new ArrayList<>();
        this.jointsFromAtrium = new ArrayList<>();
        for (int i = 0; i < polygon.getNumberSegments(); i++) {
            boolean intersect = false;
            for (ZLine testLine : testLines) {
                if (ZGeoMath.checkWB_SegmentIntersect(testLine.toWB_Segment(), polygon.getSegment(i))) {
                    intersect = true;
                    break;
                }
            }
            if (!intersect) {
//                System.out.println(i + "  " + polygon.getSegment(i).getLength());
                mainSegmentsOfAtrium.add(new ZLine(polygon.getSegment(i)));
                offsetSegmentsFromAtrium.add(ZGeoMath.offsetWB_PolygonSegment(polygon, i, corridorWidth));
            } else {
                ZLine intersectOffset = ZGeoMath.offsetWB_PolygonSegment(polygon, i, corridorWidth);
                jointsFromAtrium.add(intersectOffset.getPt0());
                jointsFromAtrium.add(intersectOffset.getPt1());
            }
        }
    }

    /**
     * switch the active control point
     *
     * @return void
     */
    public void switchActiveControl() {
        activeIndex = (activeIndex + 1) % controlPoints.size();
    }

    /**
     * update the atrium's length along the traffic graph
     *
     * @param delta update distance
     * @return void
     */
    public void updateLength(double delta) {
        controlPoints.get(activeIndex).updateDistOnEdge(delta);
        extractBoundaryPoints();
        createPolygon(vecFromCenter, boundaryPoints);
        findOffsetAndJoints(polygon, center.getLinkedEdges());
    }

    /**
     * update the atrium's width perpendicular to the traffic graph
     *
     * @param delta update distance
     * @return void
     */
    public void updateWidth(double delta) {
        distFromEdge += delta;
        controlPoints.get(activeIndex).updateDistFromEdge();
        extractBoundaryPoints();
        createPolygon(vecFromCenter, boundaryPoints);
        findOffsetAndJoints(polygon, center.getLinkedEdges());
    }

    public void addOrRemoveEscalator() {
        if (this.escalator == null) {
            this.escalator = new Escalator(this);
        } else {
            this.escalator = null;
        }
    }

    /* ------------- setter & getter (public) ------------- */

    public void setCenter(TrafficNode center) {
        this.center = center;
    }

    public void setEscalator(Escalator escalator) {
        this.escalator = escalator;
    }

    public static void setCorridorWidth(double corridorWidth) {
        AtriumOld.corridorWidth = corridorWidth;
    }

    public TrafficNode getCenter() {
        return center;
    }

    public WB_Polygon getPolygon() {
        return polygon;
    }

    public List<ZLine> getMainSegmentsOfAtrium() {
        return mainSegmentsOfAtrium;
    }

    public List<ZLine> getOffsetSegmentsFromAtrium() {
        return offsetSegmentsFromAtrium;
    }

    public List<ZPoint> getJointsFromAtrium() {
        return jointsFromAtrium;
    }

    public Escalator getEscalator() {
        return escalator;
    }

    /* ------------- draw ------------- */

    public void display(WB_Render render, PApplet app) {
        app.strokeWeight(1);
        render.drawPolygonEdges2D(polygon);
        if (this.escalator != null) {
            escalator.displayShape(render, app);
            escalator.displayCoverRegion(app);
        }
    }


    public void displayActiveControl(PApplet app) {
        app.strokeWeight(3);
        controlPoints.get(activeIndex).display(app);
    }

    /**
     * inner class
     *
     * @return
     */
    private class ControlPoint {
        private int flag;
        private ZPoint center;
        private ZPoint moveDir;
        private ZPoint oneEdgeVec = null;
        private double distOnEdge;
        private ZPoint point;
        private List<ZPoint> polyPoints;

        // on edge
        private ControlPoint(ZPoint center, ZPoint vec, double distOnEdge) {
            this.flag = 0;
            this.center = center;
            this.moveDir = vec;
            this.distOnEdge = distOnEdge;

            this.point = this.center.add(moveDir.scaleTo(this.distOnEdge));
            this.polyPoints = new ArrayList<>();
            polyPoints.add(point.add(moveDir.rotate2D(Math.PI * 0.5).scaleTo(distFromEdge)));
            polyPoints.add(point.add(moveDir.rotate2D(Math.PI * -0.5).scaleTo(distFromEdge)));
        }

        // at convex point
        private ControlPoint(ZPoint center, ZPoint vec, ZPoint oneEdgeVec) {
            this.flag = 1;
            this.center = center;
            this.moveDir = vec;
            this.oneEdgeVec = oneEdgeVec;

            double sin = Math.abs(this.oneEdgeVec.cross2D(moveDir));
            this.point = this.center.add(moveDir.scaleTo(distFromEdge / sin));
            this.polyPoints = new ArrayList<>();
            polyPoints.add(this.point);
        }

        /**
         * update the atrium's length along the traffic graph
         *
         * @param delta update distance
         * @return void
         */
        private void updateDistOnEdge(double delta) {
            distOnEdge += delta;
            if (flag == 0) {
                this.point = this.center.add(moveDir.scaleTo(this.distOnEdge));
                this.polyPoints = new ArrayList<>();
                polyPoints.add(point.add(moveDir.rotate2D(Math.PI * 0.5).scaleTo(distFromEdge)));
                polyPoints.add(point.add(moveDir.rotate2D(Math.PI * -0.5).scaleTo(distFromEdge)));
            } else {
                double sin = Math.abs(this.oneEdgeVec.cross2D(moveDir));
                this.point = this.center.add(moveDir.scaleTo(distFromEdge / sin));
                this.polyPoints = new ArrayList<>();
                polyPoints.add(this.point);
            }
        }

        /**
         * update the atrium's width perpendicular to the traffic graph
         *
         * @return void
         */
        private void updateDistFromEdge() {
            if (flag == 0) {
                this.point = center.add(moveDir.scaleTo(this.distOnEdge));
                this.polyPoints = new ArrayList<>();
                polyPoints.add(point.add(moveDir.rotate2D(Math.PI * 0.5).scaleTo(distFromEdge)));
                polyPoints.add(point.add(moveDir.rotate2D(Math.PI * -0.5).scaleTo(distFromEdge)));
            } else {
                double sin = Math.abs(this.oneEdgeVec.cross2D(moveDir));
                this.point = center.add(moveDir.scaleTo(distFromEdge / sin));
                this.polyPoints = new ArrayList<>();
                polyPoints.add(this.point);
            }
        }

        // getter
        private ZPoint getPoint() {
            return point;
        }

        // draw
        private void display(PApplet app) {
            if (polyPoints.size() == 2) {
                app.line(polyPoints.get(0).xf(), polyPoints.get(0).yf(), polyPoints.get(1).xf(), polyPoints.get(1).yf());
            }
            ZRender.drawZPoint2D(app, point, 5);
        }
    }
}
