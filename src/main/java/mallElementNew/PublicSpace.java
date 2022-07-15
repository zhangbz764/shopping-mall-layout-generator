package mallElementNew;

import advancedGeometry.ZBSpline;
import basicGeometry.ZFactory;
import basicGeometry.ZLine;
import basicGeometry.ZPoint;
import mallParameters.MallConst;
import math.ZGeoMath;
import math.ZMath;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import transform.ZTransform;
import wblut.geom.WB_Circle;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;

import java.util.*;

/**
 * main public space of shopping mall (atriums, corridors, escalators)
 *
 * @author zhangbz ZHANG Baizhou
 * @project shopping_mall
 * @date 2021/8/9
 * @time 17:03
 */
public class PublicSpace {
    private List<Atrium> atriums;
    private List<Corridor> corridors;

    private Polygon publicSpaceShapeInit;
    private Coordinate[] publicSpaceShapeBufferCtrls;
    private Polygon publicSpaceShape;

    /* ------------- constructor ------------- */

    public PublicSpace(){

    }

    public PublicSpace(Polygon[] atriumShapes, LineString mainTraffic) {
        initCorridorLines(atriumShapes, mainTraffic);
    }

    /* ------------- member function: corridors ------------- */

    /**
     * initialize main traffic corridors
     *
     * @param atriumShapes atrium shapes
     * @param mainTraffic  main traffic curve
     * @return void
     */
    public void initCorridorLines(Polygon[] atriumShapes, LineString mainTraffic) {
        // atriums are already sorted
        this.atriums = new ArrayList<>();
        for (Polygon atriumShape : atriumShapes) {
            atriums.add(new Atrium(atriumShape));
        }
        this.corridors = new ArrayList<>();
        for (int i = 0; i < atriumShapes.length - 1; i++) {
            // neighbor atrium polygons
            Polygon shape1 = atriumShapes[i];
            Polygon shape2 = atriumShapes[i + 1];
            LineString a1_ls = ZTransform.PolygonToLineString(shape1).get(0);
            LineString a2_ls = ZTransform.PolygonToLineString(shape2).get(0);
            Geometry intersection1 = a1_ls.intersection(mainTraffic);
            Geometry intersection2 = a2_ls.intersection(mainTraffic);

            // to ensure each atrium has 2 intersection points with main traffic
            if (intersection1.getGeometryType().equals("MultiPoint")
                    && intersection2.getGeometryType().equals("MultiPoint")
                    && intersection1.getNumGeometries() == 2
                    && intersection2.getNumGeometries() == 2
            ) {
                ZPoint[] pts = new ZPoint[]{
                        new ZPoint((Point) intersection1.getGeometryN(0)),
                        new ZPoint((Point) intersection1.getGeometryN(1)),
                        new ZPoint((Point) intersection2.getGeometryN(0)),
                        new ZPoint((Point) intersection2.getGeometryN(1))
                };
                double[] distAlongEdge = new double[]{
                        ZGeoMath.distAlongEdge(pts[0], pts[2], mainTraffic),
                        ZGeoMath.distAlongEdge(pts[0], pts[3], mainTraffic),
                        ZGeoMath.distAlongEdge(pts[1], pts[2], mainTraffic),
                        ZGeoMath.distAlongEdge(pts[1], pts[3], mainTraffic),
                };

                // find the closest 2 points
                int minIndex = ZMath.getMinIndex(distAlongEdge);
                ZPoint p1 = pts[(int) (minIndex * 0.5)];
                ZPoint p2 = pts[minIndex % 2 == 0 ? 2 : 3];

                // find middle points between them, and get divide line
                ZPoint mid = ZGeoMath.pointOnEdgeByDist(p1, mainTraffic, distAlongEdge[minIndex] * 0.5)[0];
                int[] edgeID = ZGeoMath.pointOnWhichEdgeIndices(mid, mainTraffic);
                ZPoint edgeVec = new ZPoint(mainTraffic.getCoordinateN(edgeID[1])).sub(new ZPoint(mainTraffic.getCoordinateN(edgeID[0])));
                ZPoint divDir = edgeVec.perpVec().normalize();
                ZLine divLine = new ZLine(mid.add(divDir.scaleTo(10)), mid.add(divDir.scaleTo(-10)));

                // create Atrium and Corridor
                Corridor c = new Corridor(divLine, MallConst.CORRIDOR_WIDTH_INIT);
                c.setNeighborAtriums(new Atrium[]{atriums.get(i), atriums.get(i + 1)});
                atriums.get(i).addNeighborCorridor(1, c);
                atriums.get(i + 1).addNeighborCorridor(0, c);
                corridors.add(c);
            }
        }

        // trim all atriums by the offset lines of corridors
        for (Atrium a : atriums) {
            Corridor[] corridors = a.neighbor;
            Corridor c0 = corridors[0];
            Corridor c1 = corridors[1];
            Coordinate[] coords = a.initShape.getCoordinates();

            if (c0 != null) {
                int min = trimAtrium(coords, c0, 1);
                a.intersectionMin[0] = min;
            }
            if (c1 != null) {
                int min = trimAtrium(coords, c1, 0);
                a.intersectionMin[1] = min;
            }

            Polygon trimShape = ZFactory.jtsgf.createPolygon(coords);
            a.trimShape = trimShape;
            a.setCurrShape(trimShape);
        }
    }

    /**
     * update 1 corridor position
     *
     * @param corridorID index of the updated corridors
     * @param newPos     new central line of the corridor
     * @return void
     */
    public void updateCorridorPos(int corridorID, ZLine newPos) {
        Corridor c = corridors.get(corridorID);
        c.setCentralLine(newPos);

        updateTrim(c);
    }

    /**
     * update 1 corridor position
     *
     * @param corridorID index of the updated corridors
     * @param width      new width of the corridor
     * @return void
     */
    public void updateCorridorWidth(int corridorID, double width) {
        Corridor c = corridors.get(corridorID);
        c.setWidth(width);

        updateTrim(c);
    }

    /**
     * update trimming step by a new Corridor
     *
     * @param c new Corridor
     * @return void
     */
    private void updateTrim(Corridor c) {
        Atrium a0 = c.neighbor[0];
        Atrium a1 = c.neighbor[1];

        Coordinate[] coords0 = a0.currShape.getCoordinates();
        int min0 = trimAtrium(coords0, c, 0);
        a0.intersectionMin[1] = min0;
        Polygon trimShape0 = ZFactory.jtsgf.createPolygon(coords0);
        a0.trimShape = trimShape0;
        a0.setCurrShape(trimShape0);

        Coordinate[] coords1 = a1.currShape.getCoordinates();
        int min1 = trimAtrium(coords1, c, 1);
        a1.intersectionMin[0] = min1;
        Polygon trimShape1 = ZFactory.jtsgf.createPolygon(coords1);
        a1.trimShape = trimShape1;
        a1.setCurrShape(trimShape1);
    }

    /**
     * core function trim the atrium shape to neighbor corridor
     *
     * @param coords   coordinates of the shape
     * @param c        corridor
     * @param offsetID which offset line of corridor should perform the trim
     * @return void
     */
    private int trimAtrium(Coordinate[] coords, Corridor c, int offsetID) {
        // find the closest segment
        double[] distToCentralLine1 = new double[coords.length - 1];
        for (int j = 0; j < coords.length - 1; j++) {
            WB_Point center = new WB_Point(
                    0.5 * (coords[j].getX() + coords[j + 1].getX()),
                    0.5 * (coords[j].getY() + coords[j + 1].getY())
            );
            distToCentralLine1[j] = WB_GeometryOp.getDistance2D(center, c.centralLine.toWB_Line());
        }
        int min = ZMath.getMinIndex(distToCentralLine1);
        // set the closest segment to intersection points
        ZLine former = new ZLine(
                coords[min == 0 ? coords.length - 2 : min - 1],
                coords[min]
        );
        ZPoint newPt1 = ZGeoMath.simpleLineElementsIntersection2D(
                former, "line", c.offset[offsetID], "line"
        );
        coords[min] = newPt1.toJtsCoordinate();
        if (min == 0) {
            coords[coords.length - 1] = coords[min];
        }
        ZLine next = new ZLine(
                coords[min + 1],
                coords[(min + 2) % (coords.length - 1)]
        );
        ZPoint newPt2 = ZGeoMath.simpleLineElementsIntersection2D(
                next, "line", c.offset[offsetID], "line"
        );
        coords[min + 1] = newPt2.toJtsCoordinate();
        if (min + 1 == coords.length - 1) {
            coords[0] = coords[min + 1];
        }

        return min;
    }

    /* ------------- member function: atrium & public shape ------------- */

    /**
     * initialize public space shape boundary
     *
     * @return void
     */
    public void initPublicShape() {
        // shape boundary
        List<Coordinate> shapeCoords = joinBoundary();
        this.publicSpaceShapeInit = ZFactory.createPolygonFromList(shapeCoords);
        updatePublicShapeBuffer(MallConst.PUBLIC_BUFFER_DIST_INIT);

        // round atriums
        for (Atrium a : atriums) {
            a.setCurrShape(ZGeoMath.roundPolygon(a.trimShape, MallConst.ATRIUM_ROUND_RADIUS_DEFAULT, 10));
        }
    }

    /**
     * update public space by a new set of control points
     *
     * @param ctrls new control points
     * @return void
     */
    public void updatePublicShape(Coordinate[] ctrls) {
        this.publicSpaceShapeBufferCtrls = ctrls;
        ZBSpline spline = new ZBSpline(publicSpaceShapeBufferCtrls, 3, 160, ZBSpline.CLOSE);
        this.publicSpaceShape = spline.getAsPolygon();
    }

    /**
     * update public space by buffer distance
     *
     * @param dist buffer distance
     * @return void
     */
    public void updatePublicShapeBuffer(double dist) {
        BufferParameters parameters = new BufferParameters(0, 1, 2, 5.0D);
        Geometry buffer = BufferOp.bufferOp(publicSpaceShapeInit, dist, parameters);
        if (buffer instanceof Polygon) {
//            Geometry sim = DouglasPeuckerSimplifier.simplify(buffer, 3);

            // remove last Coordinate
            this.publicSpaceShapeBufferCtrls = new Coordinate[buffer.getNumPoints() - 1];
            for (int i = 0; i < publicSpaceShapeBufferCtrls.length; i++) {
                publicSpaceShapeBufferCtrls[i] = buffer.getCoordinates()[i];
            }
            ZBSpline spline = new ZBSpline(publicSpaceShapeBufferCtrls, 3, 160, ZBSpline.CLOSE);
            this.publicSpaceShape = spline.getAsPolygon();
        } else {
            this.publicSpaceShape = publicSpaceShapeInit;
            // remove last Coordinate
            this.publicSpaceShapeBufferCtrls = new Coordinate[publicSpaceShapeInit.getNumPoints() - 1];
            for (int i = 0; i < publicSpaceShapeBufferCtrls.length; i++) {
                publicSpaceShapeBufferCtrls[i] = publicSpaceShapeInit.getCoordinates()[i];
            }
        }
    }

    /**
     * loop all the atriums to join the public space shape boundary
     *
     * @return java.util.List<org.locationtech.jts.geom.Coordinate>
     */
    private List<Coordinate> joinBoundary() {
        List<Coordinate> coords = new ArrayList<>();

        // the first atrium
        Atrium as = atriums.get(0);
        Coordinate[] shapeCoordsS = as.trimShape.getCoordinates();
        int min1S = as.intersectionMin[1];
        int countS = (as.intersectionMin[1] + 1) % (shapeCoordsS.length - 1);
        while (countS != min1S) {
            coords.add(shapeCoordsS[countS]);
            countS = countS == shapeCoordsS.length - 2 ? 0 : countS + 1;
        }
        coords.add(shapeCoordsS[min1S]);

        // loop forward
        for (int i = 1; i < atriums.size() - 1; i++) {
            Atrium a = atriums.get(i);
            Coordinate[] shapeCoords = a.trimShape.getCoordinates();
            int min0 = a.intersectionMin[0];
            int min1 = a.intersectionMin[1];
            int count = (min0 + 1) % (shapeCoords.length - 1);
            while (count != min1) {
                coords.add(shapeCoords[count]);
                count = count == shapeCoords.length - 2 ? 0 : count + 1;
            }
            coords.add(shapeCoords[min1]);
        }

        // the last atrium
        Atrium ae = atriums.get(atriums.size() - 1);
        Coordinate[] shapeCoordsE = ae.trimShape.getCoordinates();
        int min0E = ae.intersectionMin[0];
        int countE = (ae.intersectionMin[0] + 1) % (shapeCoordsE.length - 1);
        while (countE != min0E) {
            coords.add(shapeCoordsE[countE]);
            countE = countE == shapeCoordsE.length - 2 ? 0 : countE + 1;
        }
        coords.add(shapeCoordsE[min0E]);

        // loop backward
        for (int i = atriums.size() - 2; i > 0; i--) {
            Atrium a = atriums.get(i);
            Coordinate[] shapeCoords = a.trimShape.getCoordinates();
            int min0 = a.intersectionMin[0];
            int min1 = a.intersectionMin[1];
            int count = (min1 + 1) % (shapeCoords.length - 1);
            while (count != min0) {
                coords.add(shapeCoords[count]);
                count = count == shapeCoords.length - 2 ? 0 : count + 1;
            }
            coords.add(shapeCoords[min0]);
        }

        // close
        coords.add(coords.get(0));

        return coords;
    }

    /**
     * switch between round mode and smooth mode
     *
     * @param atriumID index of the atrium
     * @return void
     */
    public void switchAtriumRoundType(int atriumID) {
        Atrium a = atriums.get(atriumID);
        if (a.roundOrSmooth) {
            a.setCurrShape(ZGeoMath.smoothPolygon(a.trimShape, 4, 2));
        } else {
            a.setCurrShape(ZGeoMath.roundPolygon(a.trimShape, MallConst.ATRIUM_ROUND_RADIUS_DEFAULT, 10));
        }
        a.roundOrSmooth = !a.roundOrSmooth;
    }

    /**
     * update the radius of rounding atrium
     *
     * @param atriumID index of atrium
     * @param radius   new radius
     * @return void
     */
    public void updateAtriumRoundRadius(int atriumID, double radius) {
        Atrium a = atriums.get(atriumID);
        if (a.roundOrSmooth) {
            a.setCurrShape(ZGeoMath.roundPolygon(a.trimShape, radius, 10));
        }
    }

    /**
     * update smooth times of the atrium
     *
     * @param atriumID index of atrium
     * @param times    smooth count times
     * @return void
     */
    public void updateAtriumSmoothTimes(int atriumID, int times) {
        Atrium a = atriums.get(atriumID);
        if (!a.roundOrSmooth) {
            a.setCurrShape(ZGeoMath.smoothPolygon(a.trimShape, 4, times));
        }
    }

    /* ------------- member function: escalator ------------- */

    /**
     * initialize the escalators
     *
     * @param mainTraffic main traffic curve
     * @return void
     */
    public void initEscalators(LineString mainTraffic) {
        // initialize possible positions
        for (Atrium a : atriums) {
            a.possibleEscalatorBases = ZGeoMath.splitPolygonEdge(a.currShape, 64);
        }

        // find atriums to generate escalator
        Set<Atrium> generator = new HashSet<>();
        Atrium aS = atriums.get(0);
        Atrium aE = atriums.get(atriums.size() - 1);
        generator.add(aS);
        generator.add(aE);
        Point aSCenter = aS.currShape.getCentroid();
        Point aECenter = aE.currShape.getCentroid();
        WB_Point closestS = WB_GeometryOp.getClosestPoint2D(ZTransform.PointToWB_Point(aSCenter), ZTransform.LineStringToWB_PolyLine(mainTraffic));
        WB_Point closestE = WB_GeometryOp.getClosestPoint2D(ZTransform.PointToWB_Point(aECenter), ZTransform.LineStringToWB_PolyLine(mainTraffic));
        LineString cutLS = ZFactory.cutLineString2Points(mainTraffic, new ZPoint(closestS), new ZPoint(closestE));
        List<ZPoint> splitByRadius = ZGeoMath.splitPolyLineByThreshold(cutLS, 2 * MallConst.ESCALATOR_DIST_MAX, 2 * MallConst.ESCALATOR_DIST_MIN, 0);
        for (int i = 1; i < splitByRadius.size() - 1; i++) {
            double[] dists = new double[atriums.size() - 2];
            for (int j = 1; j < atriums.size() - 1; j++) {
                dists[j - 1] = WB_GeometryOp.getDistance2D(
                        splitByRadius.get(i).toWB_Point(), ZTransform.PolygonToWB_PolyLine(atriums.get(j).trimShape).get(0)
                );
            }
            int min = ZMath.getMinIndex(dists);
            generator.add(atriums.get(min + 1));
        }

        // generate escalator for each atrium
        for (Atrium a : generator) {
            generateEscalator(a);
        }
    }

    /**
     * update escalator to the next position
     *
     * @param atriumID index of atrium
     * @return void
     */
    public void updateEscalatorPos(int atriumID) {
        Atrium a = atriums.get(atriumID);

        generateEscalator(a);
    }

    /**
     * generate escalator on an atrium
     *
     * @param atrium the atrium to generate
     * @return void
     */
    private void generateEscalator(Atrium atrium) {
        // split atrium
        WB_Polygon shape = ZTransform.PolygonToWB_Polygon(atrium.currShape);
        double shapeLength = ZGeoMath.getPolyLength(shape);

        List<ZPoint> split = atrium.possibleEscalatorBases;
        int startSplitID = atrium.escalatorBaseID;

        int currSplitID = (startSplitID + 1) % split.size();
        while (currSplitID != startSplitID) {
            ZPoint p0 = split.get(currSplitID);
            WB_Point splitP = p0.toWB_Point();
            int splitEdgeIndex = ZGeoMath.pointOnWhichEdgeIndices(p0, shape)[0];
            // generate circle to find possible intersections
            WB_Circle circle = new WB_Circle(splitP, MallConst.ESCALATOR_LENGTH);
            List<WB_Point> intersection = ZGeoMath.polylineCircleIntersection(shape, circle);
            // must have intersection
            if (intersection.size() > 0) {
                double[] dists = new double[intersection.size()];
                for (int i = 0; i < intersection.size(); i++) {
                    WB_Point inter = intersection.get(i);
                    int[] onWhich = ZGeoMath.pointOnWhichEdgeIndices(new ZPoint(inter), shape);
                    int interIndex = onWhich[0];
                    if (interIndex == splitEdgeIndex) {
                        // on same edge
                        if (inter.getSqDistance2D(shape.getPoint(interIndex)) >= splitP.getSqDistance2D(shape.getPoint(interIndex))) {
                            dists[i] = inter.getDistance2D(splitP);
                        } else {
                            dists[i] = shapeLength - inter.getDistance2D(splitP);
                        }
                    } else {
                        // on different edge
                        int currIndex = splitEdgeIndex;
                        double distAlong = 0;

                        currIndex = (currIndex + 1) % shape.getNumberOfPoints();
                        distAlong += splitP.getDistance2D(shape.getPoint(currIndex));
                        while (currIndex != interIndex) {
                            distAlong += shape.getPoint(currIndex).getDistance2D(shape.getPoint((currIndex + 1) % shape.getNumberOfPoints()));
                            currIndex = (currIndex + 1) % shape.getNumberOfPoints();
                        }
                        distAlong += shape.getPoint(currIndex).getDistance2D(inter);

                        dists[i] = distAlong;
                    }
                }

                // get the closest intersection as p2
                int minIndex = ZMath.getMinIndex(dists);
                ZPoint p1 = new ZPoint(intersection.get(minIndex));

                // create escalator bound
                ZPoint sideVec = p1.sub(p0);
                ZPoint perp = sideVec.rotate2D(Math.PI * 0.5).normalize();
                Coordinate[] boundRectCoords = new Coordinate[5];
                boundRectCoords[0] = p0.toJtsCoordinate();
                boundRectCoords[1] = p1.toJtsCoordinate();
                boundRectCoords[2] = p1.add(perp.scaleTo(MallConst.ESCALATOR_WIDTH)).toJtsCoordinate();
                boundRectCoords[3] = p0.add(perp.scaleTo(MallConst.ESCALATOR_WIDTH)).toJtsCoordinate();
                boundRectCoords[4] = boundRectCoords[0];
                Polygon validRect = ZFactory.jtsgf.createPolygon(boundRectCoords);

                // validate that the escalator rectangle is within the atrium
                if (atrium.currShape.contains(validRect)) {
                    atrium.escalator = new Escalator(p0, validRect);
                    atrium.escalatorBaseID = currSplitID;
                    break;
                }
            }
            currSplitID = (currSplitID + 1) % split.size();
        }
    }

    /* ------------- setter & getter ------------- */

    public List<ZLine> getCorridorsLines() {
        List<ZLine> lines = new ArrayList<>();
        for (Corridor c : corridors) {
            lines.add(c.centralLine);
        }
        return lines;
    }

    public List<List<ZLine>> getOffsets() {
        List<List<ZLine>> offsets = new ArrayList<>();
        for (Corridor c : corridors) {
            offsets.add(Arrays.asList(c.offset));
        }
        return offsets;
    }

    public Polygon[] getAtriumTrimShapes() {
        Polygon[] shapes = new Polygon[atriums.size()];
        for (int i = 0; i < atriums.size(); i++) {
            shapes[i] = atriums.get(i).trimShape;
        }
        return shapes;
    }

    public Polygon[] getAtriumCurrShapes() {
        Polygon[] shapes = new Polygon[atriums.size()];
        for (int i = 0; i < atriums.size(); i++) {
            shapes[i] = atriums.get(i).currShape;
        }
        return shapes;
    }

    public Polygon getAtriumCurrShapeN(int n) {
        return atriums.get(n).currShape;
    }

    public double[] getAtriumCurrAreas() {
        double[] areas = new double[atriums.size()];
        for (int i = 0; i < atriums.size(); i++) {
            areas[i] = atriums.get(i).area;
        }
        return areas;
    }

    public Polygon getPublicSpaceShapeInit() {
        return publicSpaceShapeInit;
    }

    public Coordinate[] getPublicSpaceShapeBufferCtrls() {
        return publicSpaceShapeBufferCtrls;
    }

    public Polygon getPublicSpaceShape() {
        return publicSpaceShape;
    }

    public void setPublicSpaceShapeTemp(Polygon publicSpaceShape) {
        this.publicSpaceShape = publicSpaceShape;
    }

    public List<Polygon> getEscalatorBounds() {
        List<Polygon> result = new ArrayList<>();
        for (Atrium a : atriums) {
            if (a.escalator != null) {
                result.add(a.escalator.bound);
            }
        }
        return result;
    }

    public List<Integer> getEscalatorAtriumIDs() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < atriums.size(); i++) {
            if (atriums.get(i).escalator != null) {
                result.add(i);
            }
        }
        return result;
    }

    public List<ZLine> getEscalatorShapes() {
        List<ZLine> result = new ArrayList<>();
        for (Atrium a : atriums) {
            if (a.escalator != null) {
                result.addAll(a.escalator.shapes);
            }
        }
        return result;
    }

    public Polygon getEscalatorBoundN(int escalatorAtriumID) {
        return atriums.get(escalatorAtriumID).escalator.bound;
    }

    /* ------------- inner class: Corridor ------------- */

    private static class Corridor {
        private ZLine centralLine;
        private ZLine[] offset;
        private double width;
        private Atrium[] neighbor;

        private Corridor(ZLine _centralLine, double _width) {
            this.centralLine = _centralLine;
            this.width = _width;
            this.offset = centralLine.offset2D(0.5 * width);
        }

        private void setWidth(double _width) {
            this.width = _width;
            this.offset = centralLine.offset2D(0.5 * width);
            orderOffset();
        }

        private void setCentralLine(ZLine _centralLine) {
            this.centralLine = _centralLine;
            this.offset = centralLine.offset2D(0.5 * width);
            orderOffset();
        }

        private void setNeighborAtriums(Atrium[] as) {
            neighbor = as;
            orderOffset();
        }

        private void orderOffset() {
            // switch the order of the offset lines
            ZLine l0 = offset[0];
            ZLine l1 = offset[1];
            ZPoint[] test = new ZLine(
                    new ZPoint(neighbor[0].currShape.getCentroid()),
                    l0.getCenter()
            ).toLinePD();
            if (ZGeoMath.checkLineSegmentIntersect(l1.toLinePD(), test)) {
                offset = new ZLine[]{l1, l0};
            }
        }
    }

    /* ------------- inner class: Atrium ------------- */

    private static class Atrium {
        private final Polygon initShape;
        private Polygon trimShape;
        private Polygon currShape;
        private double area;
        private Corridor[] neighbor;
        private int intersectionMin[] = new int[]{-1, -1};
        private boolean roundOrSmooth = true;

        private List<ZPoint> possibleEscalatorBases;
        private int escalatorBaseID = 0;
        private Escalator escalator;

        private Atrium(Polygon _initShape) {
            this.initShape = _initShape;
            this.trimShape = _initShape;
            this.currShape = _initShape;
            this.area = _initShape.getArea();
            this.neighbor = new Corridor[]{null, null};
        }

        private void addNeighborCorridor(int index, Corridor c) {
            this.neighbor[index] = c;
        }

        public void setCurrShape(Polygon currShape) {
            this.currShape = currShape;
            this.area = currShape.getArea();
        }
    }

    /* ------------- inner class: Escalator ------------- */

    private static class Escalator {
        private ZPoint base;
        private Polygon bound;
        private List<ZLine> shapes;

        private Escalator(ZPoint _base, Polygon _bound) {
            this.base = _base;
            this.bound = _bound;

            // create shapes
            this.shapes = new ArrayList<>();
            ZPoint v01 = new ZPoint(
                    bound.getCoordinates()[1].getX() - bound.getCoordinates()[0].getX(),
                    bound.getCoordinates()[1].getY() - bound.getCoordinates()[0].getY()
            );
            ZPoint v03 = new ZPoint(
                    bound.getCoordinates()[3].getX() - bound.getCoordinates()[0].getX(),
                    bound.getCoordinates()[3].getY() - bound.getCoordinates()[0].getY()
            );
            ZPoint v01Nor = v01.normalize();
            ZPoint v03Nor = v03.normalize();
            ZLine central = new ZLine(
                    0.5 * (bound.getCoordinates()[0].getX() + bound.getCoordinates()[3].getX()),
                    0.5 * (bound.getCoordinates()[0].getY() + bound.getCoordinates()[3].getY()),
                    0.5 * (bound.getCoordinates()[1].getX() + bound.getCoordinates()[2].getX()),
                    0.5 * (bound.getCoordinates()[1].getY() + bound.getCoordinates()[2].getY())
            );
            shapes.add(central);
            ZLine sideW1 = new ZLine(
                    new ZPoint(bound.getCoordinates()[0]).add(v01Nor.scaleTo(1)),
                    new ZPoint(bound.getCoordinates()[3]).add(v01Nor.scaleTo(1))
            );
            ZLine sideW2 = new ZLine(
                    new ZPoint(bound.getCoordinates()[1]).add(v01Nor.scaleTo(-1)),
                    new ZPoint(bound.getCoordinates()[2]).add(v01Nor.scaleTo(-1))
            );
            shapes.add(sideW1);
            shapes.add(sideW2);
            ZLine sideL1 = new ZLine(
                    sideW1.getPt0().add(v03Nor.scaleTo(0.2)),
                    sideW2.getPt0().add(v03Nor.scaleTo(0.2))
            );
            ZLine sideL2 = new ZLine(
                    sideW1.getPt0().add(v03Nor.scaleTo(0.5 * MallConst.ESCALATOR_WIDTH - 0.2)),
                    sideW2.getPt0().add(v03Nor.scaleTo(0.5 * MallConst.ESCALATOR_WIDTH - 0.2))
            );
            ZLine sideL3 = new ZLine(
                    sideW1.getPt1().add(v03Nor.scaleTo(-0.2)),
                    sideW2.getPt1().add(v03Nor.scaleTo(-0.2))
            );
            ZLine sideL4 = new ZLine(
                    sideW1.getPt0().add(v03Nor.scaleTo(0.5 * MallConst.ESCALATOR_WIDTH + 0.2)),
                    sideW2.getPt0().add(v03Nor.scaleTo(0.5 * MallConst.ESCALATOR_WIDTH + 0.2))
            );
            shapes.add(sideL1);
            shapes.add(sideL2);
            shapes.add(sideL3);
            shapes.add(sideL4);
            List<ZPoint> sideL1Split = sideL1.divide(25);
            List<ZPoint> sideL2Split = sideL2.divide(25);
            for (int i = 1; i < sideL1Split.size(); i++) {
                shapes.add(new ZLine(sideL1Split.get(i), sideL2Split.get(i)));
            }
            List<ZPoint> sideL3Split = sideL3.divide(25);
            List<ZPoint> sideL4Split = sideL4.divide(25);
            for (int i = 1; i < sideL3Split.size(); i++) {
                shapes.add(new ZLine(sideL3Split.get(i), sideL4Split.get(i)));
            }
        }
    }
}