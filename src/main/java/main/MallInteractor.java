package main;

import basicGeometry.ZFactory;
import basicGeometry.ZPoint;
import mallElementNew.AtriumRaw;
import mallElementNew.AtriumRawManager;
import mallElementNew.Shop;
import mallParameters.MallConst;
import math.ZMath;
import org.locationtech.jts.geom.*;
import processing.core.PApplet;
import render.JtsRender;
import transform.ZTransform;
import wblut.geom.*;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * geometries to interact on the local canvas
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2021/5/12
 * @time 11:20
 */
public class MallInteractor {
    // site & building boundary
    private int boundaryBase = 0;                       // the L-shape base point index
    private Coordinate[] boundary_controllers;          // controller points of building boundary

    // main traffic & raw atriums
    private boolean trafficOrAtrium = true;             // switch toggle for traffic/atrium interact
    private List<WB_Point> traffic_innerControllers;    // inner controller points of traffic curve
    private List<WB_Point> traffic_entryControllers;    // entry controller points of traffic curve
    private int selectedAtriumRawType = -1;                // 选择的中庭类型代号
    private List<Polygon> atriumRawShapes;
    private Polygon selectedAtriumRawShape = null;
    private int selectedAtriumRawShapeID = -1;
    private WB_Point[] atriumRaw_controllers;              // 选择的原始中庭的控制点


    private int atriumRawDragFlag = -1;                    // 拖拽中心 or 边界控制点
    private int atriumRawNodeID = -1;                      // 被拖拽的边界控制点序号
    private Polygon mainTraffic_buffer;                 // 主路径区域（用于判断）

    // 空中走廊
    private List<WB_Point> corridorNode_interact;       // 走廊控制点
    private WB_Point[] selectedCorridorNode;            // 选择的走廊控制点
    private int selectedCorridorID = -1;                // 选择的走廊控制点序号

    // 交通空间轮廓
    private List<WB_Point> publicSpaceNode_interact;    // 交通空间轮廓控制点
    private WB_Point selectedPublicSpaceNode;           // 选择的交通空间轮廓控制点
    private Polygon[] atrium_interact;                  // 倒角后的中庭
    private Polygon selectedAtrium;                     // 选择的中庭
    private int selectedAtriumID = -1;                  // 选择的中庭序号

    // 扶梯
    private List<Polygon> escalatorBounds_interact;     // 扶梯边框
    private List<Integer> escalatorAtriumIDs;           // 扶梯所属的中庭序号
    private Polygon selectedEscalatorBound;             // 选择的扶梯边框
    private int selectedEscalatorID = -1;               // 选择的扶梯边框序号
    private int selectedEscalatorAtriumID = -1;         // 选择的扶梯所在的中庭序号

    // 柱网
    private Polygon[] rect_interact;                    // 柱网矩形
    private Polygon selectedRect;                       // 选择的柱网
    private int selectedRectID = -1;                    // 选择的柱网序号
    private WB_Point[] rectNode_interact;               // 柱网控制点
    private WB_Line[] rectCentralLine;                  // 柱网矩形轴线（用于限制控制点）

    // 商铺
    private List<Polygon> shopCell_interact;            // 商铺划格（不同层）
    private List<Polygon> selectedShopCell;             // 选择的商铺
    private List<Integer> selectedShopCellID;           // 选择的商铺序号
    private double shopArea = -1;

    // 疏散楼梯
    private List<ZPoint> generatedEvacNodes;
    private List<ZPoint> allEvacuationNodes;
    private List<Integer> generatedEvacNodeIDs;
    private ZPoint selectedEvacNode = null;
    private int selectedEvacNodeIndex = -1;
    private int newlysetEvacNodeID = -1;

    private List<Polygon> stairway_interact;
    private Polygon selectedStairway = null;
    private int selectedStairwayID = -1;

    private List<LineString> bufferCurve_interact;      // 动线边界曲线（不同层）
    private List<WB_Point> bufferCurveControl_interact;

    /* ------------- constructor ------------- */

    public MallInteractor() {

    }

    /* ------------- utils ------------- */

    public String getRentArea() {
        if (shopCell_interact != null && shopCell_interact.size() > 0) {
            shopArea = 0;
            for (Polygon p : shopCell_interact) {
                shopArea += p.getArea();
            }
            return String.format("%.2f", shopArea) + "㎡";
        } else {
            shopArea = -1;
            return "";
        }
    }

    public String getShopRatio() {
        return "";
//        return shopArea > 0 ? String.format("%.2f", (shopArea / boundary.getArea()) * 100) + "%" : "";
    }

    public String getSmallShopRatio() {
        if (shopArea > 0) {
            float small = 0;
            for (Polygon p : shopCell_interact) {
                if (p.getArea() < 150) {
                    small += 1;
                }
            }
            return String.format("%.2f", (small / shopCell_interact.size()) * 100) + "%";
        } else {
            return "";
        }
    }

    /**
     * clear active edit contents
     *
     * @param status status ID
     * @return void
     */
    public void clearActiveEdit(int status) {
        switch (status) {
            case -1:
                break;
            case 0:
                break;
            case 1:
                this.selectedAtriumRaw = null;
                this.atriumRaw_controllers = null;
                this.trafficOrAtrium = true;
                break;
            case 2:
                this.selectedCorridorID = -1;
                this.selectedCorridorNode = null;
                break;
            case 3:
                this.selectedPublicSpaceNode = null;
                this.selectedAtrium = null;
                this.selectedAtriumID = -1;
                break;
            case 4:
                this.selectedRectID = -1;
                this.selectedRect = null;
                this.rectCentralLine = null;
                this.rectNode_interact = null;
                break;
            case 5:
                this.selectedShopCell = null;
                break;
            case 6:
                break;
        }
    }

    /* ------------- site boundary interact ------------- */

    /**
     * switch 4 possible L-shape boundary
     *
     * @param siteBufferDist distance to buffer
     * @return void
     */
    public void switchBoundary(double siteBufferDist) {
        this.boundaryBase = (boundaryBase + 1) % 4;
    }

    /**
     * drag update to change the boundary shape manually
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void dragUpdateBoundary(double x, double y) {
        for (int i = 0; i < boundary_controllers.length - 1; i++) {
            Coordinate c = boundary_controllers[i];
            if (ZMath.distance2D(c.getX(), c.getY(), x, y) <= MallConst.BOUNDARY_NODE_R) {
                boundary_controllers[i] = new Coordinate(x, y);
                if (i == 0) {
                    boundary_controllers[boundary_controllers.length - 1] = new Coordinate(x, y);
                }
                break;
            }
        }
    }

    /* ------------- traffic & raw atrium interact ------------- */

    /**
     * switch the controller of traffic or atrium
     *
     * @return void
     */
    public void reverseTrafficOrAtriumRaw() {
        this.trafficOrAtrium = !trafficOrAtrium;
        if (trafficOrAtrium) {
            this.selectedAtriumRaw = null;
            this.atriumRaw_controllers = null;
        }
    }

    /**
     * initialize the AtriumRawManager by given central traffic curve
     *
     * @param _mainTrafficCurve central traffic curve
     * @return void
     */
    public void initAtriumRawManager(WB_PolyLine _mainTrafficCurve) {
        this.atriumRawManager = new AtriumRawManager(_mainTrafficCurve);
    }

    /**
     * drag update to change the main traffic or raw atriums manually
     *
     * @param x        pointer x
     * @param y        pointer y
     * @param boundary boundary polygon
     * @return void
     */
    public void dragUpdateTrafficAtriumRaw(double x, double y, Polygon boundary) {
        if (trafficOrAtrium) {
            // 主路径控制点显示
            WB_Polygon boundTemp = ZTransform.PolygonToWB_Polygon(boundary);
            for (WB_Point p : traffic_innerControllers) {
                if (ZMath.distance2D(p.xd(), p.yd(), x, y) <= MallConst.TRAFFIC_NODE_R) {
                    WB_Point point = new WB_Point(x, y);
                    if (WB_GeometryOp.contains2D(point, boundTemp) && WB_GeometryOp.getDistance2D(point, boundTemp) > MallConst.TRAFFIC_NODE_R) {
                        p.set(x, y);
                    }
                    return;
                }
            }
            for (WB_Point p : traffic_entryControllers) {
                if (ZMath.distance2D(p.xd(), p.yd(), x, y) <= MallConst.TRAFFIC_NODE_R) {
                    WB_Point point = new WB_Point(x, y);
                    p.set(WB_GeometryOp2D.getClosestPoint2D(point, ZTransform.WB_PolygonToWB_PolyLine(boundTemp).get(0)));
                    return;
                }
            }
        } else {
            // 主路径控制点关闭
            if (selectedAtriumRaw != null) {
                WB_Point center = atriumRaw_controllers[0];
                if (ZMath.distance2D(center.xd(), center.yd(), x, y) <= MallConst.ATRIUM_POS_R) {
                    WB_Point point = new WB_Point(x, y);
                    if (mainTraffic_buffer.contains(ZFactory.jtsgf.createPoint(new Coordinate(x, y)))) {
                        center.set(x, y);
                    }
                    atriumRawDragFlag = 0;
                } else {
                    for (int i = 1; i < atriumRaw_controllers.length; i++) {
                        WB_Point p = atriumRaw_controllers[i];
                        if (ZMath.distance2D(p.xd(), p.yd(), x, y) <= MallConst.ATRIUM_CTRL_R) {
                            p.set(x, y);
                            atriumRawDragFlag = 1;
                            atriumRawNodeID = i;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * release mouse to update
     *
     * @return void
     */
    public void releaseUpdateAtriumRaw() {
        if (atriumRawDragFlag == 0) {
            // move by center
            selectedAtriumRaw.moveByCenter(atriumRaw_controllers[0]);
            atriumRawManager.removeAtriumRaw(selectedAtriumRaw);
            atriumRawManager.addAtriumRaw(selectedAtriumRaw);
            for (int i = 0; i < selectedAtriumRaw.getShapePtsNum(); i++) {
                atriumRaw_controllers[i + 1] = selectedAtriumRaw.getShapePoints()[i].copy();
            }
            atriumRawManager.validateAtriumRaw();

            atriumRawDragFlag = -1;
        } else if (atriumRawDragFlag == 1) {
            // update shape
            selectedAtriumRaw.updateShapeByArea(atriumRaw_controllers[atriumRawNodeID], atriumRawNodeID - 1);
            atriumRaw_controllers[0] = selectedAtriumRaw.getCenter().copy();
            for (int i = 0; i < selectedAtriumRaw.getShapePtsNum(); i++) {
                atriumRaw_controllers[i + 1] = selectedAtriumRaw.getShapePoints()[i].copy();
            }
            atriumRawManager.validateAtriumRaw();

            atriumRawDragFlag = -1;
            atriumRawNodeID = -1;
        }
    }

    /**
     * select / unselect atrium raw shape by mouse click
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectAtriumRawShape(double x, double y) {
        Point mouse = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
        if (selectedAtriumRawShapeID > -1) {
            if (!selectedAtriumRawShape.contains(mouse)) {
                selectedAtriumRawShape = null;
                selectedAtriumRawShapeID = -1;
                atriumRaw_controllers = null;
            }
        } else {
            for (int i = 0; i < atriumRawShapes.size(); i++) {
                Polygon shape = atriumRawShapes.get(i);
                if (shape.contains(mouse)) {
                    this.selectedAtriumRawShape = shape;
                    this.selectedAtriumRawShapeID = i;
                    break;
                }
            }
        }
    }

    /**
     * update selected raw atrium shape center and controllers
     *
     * @param center center of selected raw atrium
     * @param ctrls  controllers of selected raw atrium
     * @return void
     */
    public void updateAtriumRawControls(WB_Point center, WB_Point[] ctrls) {
        this.atriumRaw_controllers = new WB_Point[ctrls.length + 1];
        atriumRaw_controllers[0] = center.copy();
        for (int i = 0; i < ctrls.length; i++) {
            atriumRaw_controllers[i + 1] = ctrls[i].copy();
        }
    }

    /* ------------- main corridor interact ------------- */

    /**
     * select corridor node
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectCorridorNode(double x, double y) {
        if (selectedCorridorNode == null) {
            for (int i = 0; i < corridorNode_interact.size(); i++) {
                WB_Point p0, p1;

                WB_Point curr = corridorNode_interact.get(i);
                if (ZMath.distance2D(curr.xd(), curr.yd(), x, y) <= MallConst.CORRIDOR_NODE_R) {
                    if (i % 2 == 0) {
                        p0 = curr;
                        p1 = corridorNode_interact.get(i + 1);
                    } else {
                        p1 = curr;
                        p0 = corridorNode_interact.get(i - 1);
                    }
                    this.selectedCorridorNode = new WB_Point[]{p0, p1};
                    this.selectedCorridorID = (int) (i * 0.5);
                    break;
                }
            }
        } else {
            WB_Point p0 = selectedCorridorNode[0];
            WB_Point p1 = selectedCorridorNode[1];
            if (ZMath.distance2D(p0.xd(), p0.yd(), x, y) > MallConst.CORRIDOR_NODE_R
                    &&
                    ZMath.distance2D(p1.xd(), p1.yd(), x, y) > MallConst.CORRIDOR_NODE_R
            ) {
                selectedCorridorNode = null;
                selectedCorridorID = -1;
            }
        }
    }

    /**
     * drag to change corridor node
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void dragUpdateCorridor(double x, double y) {
        if (selectedCorridorNode != null && selectedCorridorID > -1) {
            for (WB_Point p : selectedCorridorNode) {
                if (ZMath.distance2D(p.xd(), p.yd(), x, y) <= MallConst.CORRIDOR_NODE_R) {
                    WB_Point point = new WB_Point(x, y);
                    p.set(x, y);
                    break;
                }
            }
        }
    }

    /* ------------- public space interact ------------- */

    /**
     * select public space node
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectPublicSpaceNodeOrAtrium(double x, double y) {
        if (selectedPublicSpaceNode == null) {
            for (int i = 0; i < publicSpaceNode_interact.size(); i++) {
                WB_Point curr = publicSpaceNode_interact.get(i);
                if (ZMath.distance2D(curr.xd(), curr.yd(), x, y) <= MallConst.PUBLIC_SPACE_NODE_R) {
                    this.selectedPublicSpaceNode = curr;
                    break;
                }
            }
        } else {
            if (ZMath.distance2D(selectedPublicSpaceNode.xd(), selectedPublicSpaceNode.yd(), x, y) > MallConst.PUBLIC_SPACE_NODE_R) {
                selectedPublicSpaceNode = null;
            }
        }

        Point mouse = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
        if (selectedAtrium == null) {
            for (int i = 0; i < atrium_interact.length; i++) {
                Polygon p = atrium_interact[i];
                if (p.contains(mouse)) {
                    selectedAtrium = p;
                    selectedAtriumID = i;
                    break;
                }
            }
        } else {
            if (!selectedAtrium.contains(mouse)) {
                selectedAtrium = null;
                selectedAtriumID = -1;
            }
        }
    }

    /**
     * remove public space node
     *
     * @return void
     */
    public void removePublicSpaceNode() {
        if (selectedPublicSpaceNode != null) {
            publicSpaceNode_interact.remove(selectedPublicSpaceNode);
            selectedPublicSpaceNode = null;
        }
    }

    /* ------------- escalator interact ------------- */

    /**
     * select escalator bound
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectEscalator(double x, double y) {
        Point mouse = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
        if (selectedEscalatorBound == null) {
            for (int i = 0; i < escalatorBounds_interact.size(); i++) {
                Polygon bound = escalatorBounds_interact.get(i);
                if (bound.contains(mouse)) {
                    this.selectedEscalatorBound = bound;
                    this.selectedEscalatorID = i;
                    this.selectedEscalatorAtriumID = escalatorAtriumIDs.get(i);
                    break;
                }
            }
        } else {
            if (!selectedEscalatorBound.contains(mouse)) {
                this.selectedEscalatorBound = null;
                this.selectedEscalatorID = -1;
                this.selectedEscalatorAtriumID = -1;
            }
        }
    }

    /* ------------- structure grid interact ------------- */

    /**
     * select grid rectangle
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectGridRect(double x, double y) {
        Point mouse = ZFactory.jtsgf.createPoint(new Coordinate(x, y));

        if (selectedRect == null) {
            for (int i = 0; i < rect_interact.length; i++) {
                Polygon p = rect_interact[i];
                if (p.contains(mouse)) {
                    selectedRect = p;
                    selectedRectID = i;
                    rectNode_interact = new WB_Point[4];
                    for (int j = 0; j < rectNode_interact.length; j++) {
                        Coordinate c1 = p.getCoordinates()[j];
                        Coordinate c2 = p.getCoordinates()[j + 1];
                        rectNode_interact[j] = new WB_Point(0.5 * (c1.x + c2.x), 0.5 * (c1.y + c2.y));
                    }
                    rectCentralLine = new WB_Line[2];
                    for (int j = 0; j < rectCentralLine.length; j++) {
                        WB_Point p1 = rectNode_interact[j];
                        WB_Point p2 = rectNode_interact[j + 2];
                        rectCentralLine[j] = new WB_Line(p1, p2.sub(p1));
                    }
                    break;
                }
            }
        } else {
            if (!selectedRect.contains(mouse)) {
                unselectGridRect();
            }
        }
    }

    /**
     * clear selected grid rectangle
     *
     * @return void
     */
    public void unselectGridRect() {
        selectedRect = null;
        selectedRectID = -1;
        rectCentralLine = null;
        rectNode_interact = null;
    }

    /**
     * drag to update grid rectangle
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void dragUpdateGrid(double x, double y) {
        if (selectedRect != null) {
            for (int i = 0; i < rectNode_interact.length; i++) {
                WB_Point p = rectNode_interact[i];
                if (ZMath.distance2D(p.xd(), p.yd(), x, y) <= MallConst.STRUCTURE_CTRL_R) {
                    Coordinate[] coords = selectedRect.getCoordinates();
                    double[] d1 = new double[]{coords[i].x - p.xd(), coords[i].y - p.yd()};
                    double[] d2 = new double[]{coords[i + 1].x - p.xd(), coords[i + 1].y - p.yd()};

                    p.set(WB_GeometryOp.getClosestPoint2D(new WB_Point(x, y), rectCentralLine[i & 1]));

                    // set coordinates
                    coords[i].setCoordinate(new Coordinate(p.xd() + d1[0], p.yd() + d1[1]));
                    coords[i + 1].setCoordinate(new Coordinate(p.xd() + d2[0], p.yd() + d2[1]));
                    if (i == 0) {
                        coords[4] = coords[i];
                    } else if (i == 3) {
                        coords[0] = coords[i + 1];
                    }

                    // rebuild rectangle
                    selectedRect = ZFactory.jtsgf.createPolygon(coords);
                    rect_interact[selectedRectID] = selectedRect;

                    // set interact nodes and central lines
                    int i1 = (i + 1) % rectNode_interact.length;
                    int i2 = (i + 3) % rectNode_interact.length;
                    rectNode_interact[i1].set(0.5 * (coords[i1].x + coords[i1 + 1].x), 0.5 * (coords[i1].y + coords[i1 + 1].y));
                    rectNode_interact[i2].set(0.5 * (coords[i2].x + coords[i2 + 1].x), 0.5 * (coords[i2].y + coords[i2 + 1].y));

                    rectCentralLine[((i & 1) + 1) % 2] = new WB_Line(rectNode_interact[i1], rectNode_interact[i2].sub(rectNode_interact[i1]));
                    break;
                }
            }
        }
    }

    /* ------------- shop cell interact ------------- */

    /**
     * select shop cell
     *
     * @param x pointer x
     * @param y pointer y
     * @return void
     */
    public void selectShopCell(double x, double y) {
        int size = shopCell_interact.size();
        Point pointer = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
        for (int i = 0; i < size; i++) {
            Polygon cell = shopCell_interact.get(i);
            if (cell.contains(pointer)) {
                if (this.selectedShopCellID.contains(i)) {
                    selectedShopCell.remove(cell);
                    selectedShopCellID.remove((Integer) i);
                } else {
                    selectedShopCell.add(cell);
                    selectedShopCellID.add(i);
                }
                break;
            }
        }
    }

    /**
     * union selected shop cells
     *
     * @return void
     */
    public void unionShopCell() {
        if (selectedShopCell.size() > 1) {
            shopCell_interact.removeAll(selectedShopCell);

            Polygon[] polygons = new Polygon[selectedShopCell.size()];
            for (int i = 0; i < selectedShopCell.size(); i++) {
                polygons[i] = selectedShopCell.get(i);
            }
            GeometryCollection collection = ZFactory.jtsgf.createGeometryCollection(polygons);
            Geometry union = collection.buffer(0);

            if (union instanceof Polygon) {
                shopCell_interact.add((Polygon) union);
            } else if (union instanceof MultiPolygon) {
                for (int i = 0; i < union.getNumGeometries(); i++) {
                    shopCell_interact.add((Polygon) union.getGeometryN(i));
                }
            } else {
                System.out.println(union.getGeometryType());
            }
        }
        selectedShopCell.clear();
        selectedShopCellID.clear();
    }

    /* ------------- evacuation stairway interact ------------- */

    /**
     * drag update the evacuation generator position
     *
     * @param x
     * @param y
     * @param boundary
     * @return void
     */
    public void dragUpdateEvacNode(double x, double y, Polygon boundary) {
//        for (ZPoint evacNode : generatedEvacNodes) {
//            if (ZMath.distance2D(evacNode.xd(), evacNode.yd(), x, y) <= MallConst.EVACATION_NODE_R) {
//                evacNode.set(x, y);
//                break;
//            }
//        }
        WB_Polygon boundTemp = ZTransform.PolygonToWB_Polygon(boundary);
        for (int i = 0; i < generatedEvacNodes.size(); i++) {
            ZPoint evacNode = generatedEvacNodes.get(i);
            if (ZMath.distance2D(evacNode.xd(), evacNode.yd(), x, y) <= MallConst.EVACATION_NODE_R) {
                this.selectedEvacNode = evacNode;
                this.selectedEvacNodeIndex = i;
                WB_Point point = new WB_Point(x, y);
                selectedEvacNode.set(WB_GeometryOp2D.getClosestPoint2D(point, ZTransform.WB_PolygonToWB_PolyLine(boundTemp).get(0)));
                break;
            }
        }

//        for (int i = 0; i < generatedEvacNodes.size(); i++) {
//            ZPoint evacNode = generatedEvacNodes.get(i);
//            if (ZMath.distance2D(evacNode.xd(), evacNode.yd(), x, y) <= MallConst.EVACATION_NODE_R) {
//                this.selectedEvacNodeID = generatedEvacNodeIDs.get(i);
//                double[] dist = new double[allEvacuationNodes.size()];
//                for (int j = 0; j < dist.length; j++) {
//                    dist[j] = allEvacuationNodes.get(j).distanceSq(evacNode);
//                }
//                this.newlysetEvacNodeID = ZMath.getMinIndex(dist);
//                ZPoint newEvacNode = allEvacuationNodes.get(newlysetEvacNodeID);
//                generatedEvacNodes.set(i, newEvacNode);
//                break;
//            }
//        }
    }

    public void releaseUpdateEvacNode() {
        if (selectedEvacNode != null) {
            double[] dist = new double[allEvacuationNodes.size()];
            for (int j = 0; j < dist.length; j++) {
                dist[j] = allEvacuationNodes.get(j).distanceSq(selectedEvacNode);
            }
            this.newlysetEvacNodeID = ZMath.getMinIndex(dist);

            generatedEvacNodes.set(selectedEvacNodeIndex, allEvacuationNodes.get(newlysetEvacNodeID));
            generatedEvacNodeIDs.set(selectedEvacNodeIndex, newlysetEvacNodeID);

        }
    }

    public void clearSelEvac() {
        selectedEvacNode = null;
        selectedEvacNodeIndex = -1;
        newlysetEvacNodeID = -1;
    }

    public void selectStairwayShape(double x, double y) {
        if (stairway_interact != null) {
            Point pointer = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
            if (selectedStairwayID < 0) {
                for (int i = 0; i < stairway_interact.size(); i++) {
                    Polygon stair = stairway_interact.get(i);
                    if (stair.contains(pointer)) {
                        selectedStairwayID = i;
                        selectedStairway = stair;
                        break;
                    }
                }
            } else {
                if (!selectedStairway.contains(pointer)) {
                    clearSelecteStairway();
                }
            }
        }
    }

    public void clearSelecteStairway() {
        selectedStairwayID = -1;
        selectedStairway = null;
    }

    /* ------------- buffer curve shape interact ------------- */

    public void switchFloor(char floorKey) {

    }

    public void drawBufferCurve(PApplet app, JtsRender jtsRender) {
        app.pushStyle();

        // draw curve
        if (bufferCurve_interact != null) {
            app.stroke(25, 200, 202);
            app.strokeWeight(3);
            for (LineString ls : bufferCurve_interact) {
                jtsRender.drawGeometry(ls);
            }
        }

        app.popStyle();
    }

    public void drawBufferCurveControls(PApplet app, WB_Render render) {
        app.pushStyle();

        // draw Curve control points
        if (bufferCurveControl_interact != null) {
            app.fill(0, 0, 255);
            app.noStroke();
            for (WB_Point p : bufferCurveControl_interact) {
                render.drawPoint2D(p);
            }
        }

        app.popStyle();
    }

    /* ------------- setter & getter ------------- */

    public int getBoundaryBase() {
        return boundaryBase;
    }

    public void setBoundary_controllers(Coordinate[] boundary_controllers) {
        this.boundary_controllers = boundary_controllers;
    }

    public Coordinate[] getBoundary_controllers() {
        return boundary_controllers;
    }

    public boolean getTrafficOrAtrium() {
        return trafficOrAtrium;
    }

    public void setTraffic_innerControllers(List<WB_Point> traffic_innerControllers) {
        this.traffic_innerControllers = traffic_innerControllers;
    }

    public void setTraffic_entryControllers(List<WB_Point> traffic_entryControllers) {
        this.traffic_entryControllers = traffic_entryControllers;
    }

    public List<WB_Point> getTraffic_innerControllers() {
        return traffic_innerControllers;
    }

    public List<WB_Point> getTraffic_entryControllers() {
        return traffic_entryControllers;
    }

    public void setMainTraffic_buffer(Polygon mainTraffic_buffer) {
        this.mainTraffic_buffer = mainTraffic_buffer;
    }

    public void setSelectedAtriumRawType(int selectedAtriumRawType) {
        this.selectedAtriumRawType = selectedAtriumRawType;
    }

    public int getSelectedAtriumRawType() {
        return selectedAtriumRawType;
    }

    public void setAtriumRawShapes(List<Polygon> atriumRawShapes) {
        this.atriumRawShapes = atriumRawShapes;
    }

    public int getSelectedAtriumRawShapeID() {
        return selectedAtriumRawShapeID;
    }

    public Polygon[] getAtriumRawShapes() {
        Polygon[] atriums = new Polygon[atriumRawManager.getNumAtriumRaw()];
        for (int i = 0; i < atriums.length; i++) {
            AtriumRaw a = atriumRawManager.getAtriumRaws().get(i);
            atriums[i] = a.getShape();
        }
        return atriums;
    }

    public void setCorridorNode_interact(List<WB_Point> corridorNode_interact) {
        this.corridorNode_interact = corridorNode_interact;
    }

    public List<WB_Point> getAllCorridorNode_interact() {
        return corridorNode_interact;
    }

    public int getSelectedCorridorID() {
        return selectedCorridorID;
    }

    public WB_Point[] getSelectedCorridorNode() {
        return selectedCorridorNode;
    }

    public void setPublicSpaceNode_interact(Coordinate[] publicSpaceCurveCtrls) {
        this.publicSpaceNode_interact = new ArrayList<>();
        for (Coordinate c : publicSpaceCurveCtrls) {
            publicSpaceNode_interact.add(ZTransform.CoordinateToWB_Point(c));
        }
    }

    public List<WB_Point> getPublicSpaceNode_interact() {
        return publicSpaceNode_interact;
    }

    public void setAtrium_interact(Polygon[] atrium_interact) {
        this.atrium_interact = atrium_interact;
    }

    public int getSelectedAtriumID() {
        return selectedAtriumID;
    }

    public void setChangedAtrium(Polygon newAtriumShape, int index) {
        selectedAtrium = newAtriumShape;
        atrium_interact[index] = newAtriumShape;
    }

    public void setEscalatorBounds_interact(List<Polygon> escalatorBounds_interact) {
        this.escalatorBounds_interact = escalatorBounds_interact;
    }

    public void setEscalatorAtriumIDs(List<Integer> escalatorAtriumIDs) {
        this.escalatorAtriumIDs = escalatorAtriumIDs;
    }

    public int getSelectedEscalatorAtriumID() {
        return selectedEscalatorAtriumID;
    }

    public void setEscalatorBound_interact(Polygon bound) {
        this.selectedEscalatorBound = bound;
        this.escalatorBounds_interact.set(selectedEscalatorID, bound);
    }

    public void setRect_interact(Polygon[] rect_interact) {
        this.rect_interact = rect_interact;
    }

    public Polygon getSelectedRect() {
        return selectedRect;
    }

    public int getSelectedRectID() {
        return selectedRectID;
    }

    public List<Integer> getSelectedShopCellID() {
        return selectedShopCellID;
    }

    public void setShopCell_interact(List<Shop> currentShops) {
        this.shopCell_interact = new ArrayList<>();
        for (Shop shop : currentShops) {
            shopCell_interact.add(shop.getShape());
        }
        this.selectedShopCell = new ArrayList<>();
        this.selectedShopCellID = new ArrayList<>();
    }

    public List<Polygon> getShopCell_interact() {
        return shopCell_interact;
    }

    public void setGeneratedEvacNodes(List<ZPoint> evacuationNodes, List<Integer> ids) {
        this.generatedEvacNodes = evacuationNodes;
        this.generatedEvacNodeIDs = ids;
    }

    public List<ZPoint> getGeneratedEvacNodes() {
        return generatedEvacNodes;
    }

    public List<Integer> getGeneratedEvacNodeIDs() {
        return generatedEvacNodeIDs;
    }

    public void setAllEvacuationNodes(List<ZPoint> allEvacuationNodes) {
        this.allEvacuationNodes = allEvacuationNodes;
    }

    public int getSelectedEvacNodeIndex() {
        return selectedEvacNodeIndex;
    }

    public int getNewlysetEvacNodeID() {
        return newlysetEvacNodeID;
    }

    public void setStairway_interact(List<Polygon> stairway_interact) {
        this.stairway_interact = stairway_interact;
    }

    public int getSelectedStairwayID() {
        return selectedStairwayID;
    }

    /* ------------- draw ------------- */

    public void displayLocal(PApplet app, WB_Render render, JtsRender jtsRender, int status) {
        app.pushStyle();
        switch (status) {
            case -1:
                break;
            case MallConst.E_SITE_BOUNDARY:
                displaySiteBoundary(app, jtsRender);
                break;
            case MallConst.E_TRAFFIC_ATRIUM:
                if (trafficOrAtrium) {
                    displayTraffic(app, render);
                }
                displayRawAtrium(app, jtsRender, render);
                if (selectedAtriumRaw != null) {
                    displaySelectedAtriumRaw(app, jtsRender);
                }
                break;
            case MallConst.E_MAIN_CORRIDOR:
                displayCorridorNode(app);
                if (selectedCorridorNode != null) {
                    displaySelectedCorridorNode(app);
                }
                break;
            case MallConst.E_PUBLIC_SPACE:
                displayPublicSpaceNodes(app);
                if (selectedPublicSpaceNode != null) {
                    displaySelectedPublicNodes(app);
                }
                if (selectedAtrium != null) {
                    displaySelectedAtrium(app, jtsRender);
                }
                break;
            case MallConst.E_ESCALATOR:
                if (selectedEscalatorBound != null) {
                    displaySelectedEscalator(app, jtsRender);
                }
                break;
            case MallConst.E_STRUCTURE_GRID:
                if (selectedRect != null) {
                    displaySelectedGrid(app, jtsRender);
                }
                break;
            case MallConst.E_SHOP_EDIT:
                if (selectedShopCell != null) {
                    displaySelectedCell(app, jtsRender);
                }
                break;
            case MallConst.E_EVAC_STAIRWAY:
                displayEvacPos(app);
                if (selectedStairway != null) {
                    displaySelEvacShape(app, jtsRender);
                }
                break;
            case MallConst.E_WASHROOM:

                break;
        }
        app.popStyle();
    }

    private void displaySiteBoundary(PApplet app, JtsRender render) {
        // draw boundary control nodes
        app.noStroke();
        app.fill(255, 97, 136);
        for (Coordinate p : boundary_controllers) {
            app.ellipse((float) p.getX(), (float) p.getY(), (float) MallConst.BOUNDARY_NODE_R, (float) MallConst.BOUNDARY_NODE_R);
        }
    }

    private void displayTraffic(PApplet app, WB_Render render) {
        // draw traffic control nodes
        app.noStroke();
        app.fill(255, 97, 136);
        for (WB_Point p : traffic_innerControllers) {
            app.ellipse(p.xf(), p.yf(), (float) MallConst.TRAFFIC_NODE_R, (float) MallConst.TRAFFIC_NODE_R);
        }
        app.fill(128);
        for (WB_Point p : traffic_entryControllers) {
            app.ellipse(p.xf(), p.yf(), (float) MallConst.TRAFFIC_NODE_R, (float) MallConst.TRAFFIC_NODE_R);
        }
        app.stroke(128);
        app.strokeWeight(0.5f);
        for (int i = 0; i < traffic_innerControllers.size() - 1; i++) {
            app.line(
                    traffic_innerControllers.get(i).xf(), traffic_innerControllers.get(i).yf(),
                    traffic_innerControllers.get(i + 1).xf(), traffic_innerControllers.get(i + 1).yf()
            );
        }
        app.line(
                traffic_entryControllers.get(0).xf(), traffic_entryControllers.get(0).yf(),
                traffic_innerControllers.get(0).xf(), traffic_innerControllers.get(0).yf()
        );
        app.line(
                traffic_entryControllers.get(1).xf(), traffic_entryControllers.get(1).yf(),
                traffic_innerControllers.get(traffic_innerControllers.size() - 1).xf(), traffic_innerControllers.get(traffic_innerControllers.size() - 1).yf()
        );
    }

    private void displayRawAtrium(PApplet app, JtsRender jtsRender, WB_Render render) {
        // draw all raw atriums
        if (atriumRawManager.getValidAtriumRaw()) {
            app.stroke(55, 103, 171);
        } else {
            app.stroke(255, 0, 0);
        }
        app.strokeWeight(2);
        app.noFill();

        for (AtriumRaw ar : atriumRawManager.getAtriumRaws()) {
            jtsRender.drawGeometry(ar.getShape());
        }

        app.fill(255);
        app.textSize(2);
        for (int i = 0; i < atriumRawManager.getNumAtriumRaw(); i++) {
            Polygon p = atriumRawManager.getAtriumRaws().get(i).getShape();
            app.pushMatrix();
            app.scale(1, -1);
            app.translate(0, (float) (-2 * p.getCentroid().getY()));
            app.text(String.format("%.2f", p.getArea()), (float) p.getCentroid().getX(), (float) p.getCentroid().getY());
            app.popMatrix();
        }
    }

    private void displaySelectedAtriumRaw(PApplet app, JtsRender render) {
        // draw center
        app.noStroke();
        app.fill(255, 97, 136);
        app.ellipse(atriumRaw_controllers[0].xf(), atriumRaw_controllers[0].yf(), MallConst.ATRIUM_POS_R, MallConst.ATRIUM_POS_R);

        // draw control points and lines
        app.fill(169, 210, 118);
        for (int i = 1; i < atriumRaw_controllers.length; i++) {
            app.ellipse(atriumRaw_controllers[i].xf(), atriumRaw_controllers[i].yf(), MallConst.ATRIUM_CTRL_R, MallConst.ATRIUM_CTRL_R);
        }
        app.stroke(128);
        app.strokeWeight(0.5f);
        for (int i = 1; i < atriumRaw_controllers.length - 1; i++) {
            app.line(
                    atriumRaw_controllers[i].xf(), atriumRaw_controllers[i].yf(),
                    atriumRaw_controllers[i + 1].xf(), atriumRaw_controllers[i + 1].yf()
            );
        }
        app.line(
                atriumRaw_controllers[1].xf(), atriumRaw_controllers[1].yf(),
                atriumRaw_controllers[atriumRaw_controllers.length - 1].xf(), atriumRaw_controllers[atriumRaw_controllers.length - 1].yf()
        );

        // draw shape
        app.stroke(0, 255, 0);
        app.strokeWeight(4);
        app.noFill();
        render.drawGeometry(selectedAtriumRaw.getShape());
    }

    private void displayCorridorNode(PApplet app) {
        app.stroke(0, 255, 0);
        for (int i = 0; i < corridorNode_interact.size(); i += 2) {
            WB_Point p1 = corridorNode_interact.get(i);
            WB_Point p2 = corridorNode_interact.get(i + 1);
            app.line(p1.xf(), p1.yf(), p2.xf(), p2.yf());
        }
        app.noStroke();
        app.fill(80);
        for (WB_Point p : corridorNode_interact) {
            app.ellipse(p.xf(), p.yf(), (float) MallConst.CORRIDOR_NODE_R, (float) MallConst.CORRIDOR_NODE_R);
        }
    }

    private void displaySelectedCorridorNode(PApplet app) {
        app.fill(255, 97, 136);
        for (WB_Point p : selectedCorridorNode) {
            app.ellipse(p.xf(), p.yf(), (float) MallConst.CORRIDOR_NODE_R, (float) MallConst.CORRIDOR_NODE_R);
        }
    }

    private void displayPublicSpaceNodes(PApplet app) {
//        app.noStroke();
//        app.fill(169, 210, 118);
//        for (WB_Point p : publicSpaceNode_interact) {
//            app.ellipse(p.xf(), p.yf(), MallConst.ATRIUM_CTRL_R, MallConst.ATRIUM_CTRL_R);
//        }
//        app.stroke(128);
//        app.strokeWeight(0.5f);
//        for (int i = 0; i < publicSpaceNode_interact.size(); i++) {
//            app.line(
//                    publicSpaceNode_interact.get(i).xf(), publicSpaceNode_interact.get(i).yf(),
//                    publicSpaceNode_interact.get((i + 1) % publicSpaceNode_interact.size()).xf(), publicSpaceNode_interact.get((i + 1) % publicSpaceNode_interact.size()).yf()
//            );
//        }
    }

    private void displaySelectedPublicNodes(PApplet app) {
        app.fill(255, 97, 136);
        app.ellipse(selectedPublicSpaceNode.xf(), selectedPublicSpaceNode.yf(), MallConst.ATRIUM_CTRL_R, MallConst.ATRIUM_CTRL_R);
    }

    private void displaySelectedAtrium(PApplet app, JtsRender jtsRender) {
        app.noFill();
        app.stroke(0, 255, 0);
        app.strokeWeight(4);
        jtsRender.drawGeometry(selectedAtrium);
    }

    private void displaySelectedGrid(PApplet app, JtsRender jtsRender) {
        // draw rect
        app.stroke(0, 255, 0);
        app.strokeWeight(4);
        app.noFill();
        jtsRender.drawGeometry(selectedRect);

        // draw control points
        app.noStroke();
        app.fill(255, 97, 136);
        for (WB_Point p : rectNode_interact) {
            app.ellipse(p.xf(), p.yf(), MallConst.STRUCTURE_CTRL_R, MallConst.STRUCTURE_CTRL_R);
        }
    }

    private void displaySelectedCell(PApplet app, JtsRender jtsRender) {
        app.noFill();
        app.stroke(0, 255, 0);
        app.strokeWeight(5);
        for (Polygon p : selectedShopCell) {
            jtsRender.drawGeometry(p);
        }
    }

    private void displaySelectedEscalator(PApplet app, JtsRender jtsRender) {
        app.noFill();
        app.stroke(0, 255, 0);
        app.strokeWeight(5);
        jtsRender.drawGeometry(selectedEscalatorBound);
    }

    private void displayEvacPos(PApplet app) {
        app.noStroke();
        app.fill(255, 97, 136);
        for (ZPoint p : generatedEvacNodes) {
            app.ellipse(p.xf(), p.yf(), (float) MallConst.EVACATION_NODE_R, (float) MallConst.EVACATION_NODE_R);
        }
    }

    private void displaySelEvacShape(PApplet app, JtsRender jtsRender) {
        app.pushMatrix();
        app.translate(0, 0, 0.5f);
        app.noFill();
        app.stroke(0, 255, 0);
        app.strokeWeight(5);
        jtsRender.drawGeometry(selectedStairway);
        app.popMatrix();
    }
}
