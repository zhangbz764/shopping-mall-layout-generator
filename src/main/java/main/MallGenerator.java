package main;

import advancedGeometry.rectCover.ZRectCover;
import basicGeometry.ZFactory;
import basicGeometry.ZLine;
import mallElementNew.*;
import mallParameters.MallConst;
import math.ZGeoMath;
import org.locationtech.jts.geom.*;
import processing.core.PApplet;
import render.JtsRender;
import render.ZRender;
import transform.ZTransform;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Point;
import wblut.geom.WB_PolyLine;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * main generator
 * catch data from frontend
 * convert data to frontend
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project archijson
 * @date 2021/3/10
 * @time 17:55
 */
public class MallGenerator {
    // SiteBase: site & boundary
    private SiteBase siteBase;
    private double boundaryArea = 0;

    // MainTraffic: main traffic & raw atriums
    private MainTraffic mainTraffic;
    private AtriumRawManager atriumRawManager;
    private LineString innerTrafficCurve;          // 内部主路径轴线（除去入口）

    // PublicSpace: main corridor & public space
    private Polygon[] rawAtrium_receive;       // 中庭多边形（共用）
    private PublicSpace publicSpace;

    // structure grid
    private StructureGrid[] grids;                  // 结构轴网（共用）
    private boolean validGrids = true;              // 轴网是否覆盖平面
    private boolean gridModelSwitch = true;         // 8.4m 或 9m 切换

    // ShopManager: shop cells
    private ShopManager shopManager;

//    // core generate result: floors
//    private MallFloor[] floors;

    // AuxiliarySpace: evacuation stairways & washrooms
    private AuxiliarySpace auxiliarySpace;


    private List<List<LineString>> bufferCurve_receive;   // 动线边界曲线（不同层）
//    private List<List<WB_Polygon>> cellPolys_receive;     // 商铺剖分多边形（不同层）

    /* ------------- constructor ------------- */

    public MallGenerator() {

    }

    /* ------------- utils ------------- */

    public String getTrafficLength() {
        String s = "";
        if (mainTraffic != null) {
            s = s + String.format("%.2f", mainTraffic.getTrafficLength()) + "m";
        }
        return s;
    }

    public String getInnerTrafficLength() {
        String s = "";
        if (innerTrafficCurve != null) {
            s = s + String.format("%.2f", innerTrafficCurve.getLength()) + "m";
        }
        return s;
    }

    /* ------------- generating site & boundary ------------- */

    /**
     * initialize site and boundary
     *
     * @param _site     input site
     * @param _boundary input boundary (null allowed)
     * @param param1    input parameter 1 (int)
     * @param param2    input parameter 2 (double)
     * @param param3    input parameter 3 (double)
     * @return void
     */
    public void initSiteBoundary(WB_Polygon _site, WB_Polygon _boundary, int param1, double param2, double param3) {
        if (_boundary == null) {
            // init SiteBase_L
            this.siteBase = new SiteBase_L(
                    ZTransform.WB_PolygonToPolygon(_site),
                    param1, param2, param3
            );
        } else {
            this.siteBase = new SiteBase_Input(
                    ZTransform.WB_PolygonToPolygon(_site),
                    ZTransform.WB_PolygonToPolygon(_boundary)
            );
        }
        this.boundaryArea = siteBase.getBoundaryArea();
    }

    /**
     * update SiteBase_L parameters
     *
     * @param base           base point (L shape)
     * @param redLineDist    red line distance
     * @param siteBufferDist L shape buffer distance
     * @return void
     */
    public void updateSiteBaseL(int base, double redLineDist, double siteBufferDist) {
        siteBase.updateByParams(base, redLineDist, siteBufferDist);
        this.boundaryArea = siteBase.getBoundaryArea();
    }

    /**
     * update boundary by nodes
     *
     * @param boundaryNodes_receive nodes received from MallInteract
     * @return void
     */
    public void updateBoundaryByNodes(Coordinate[] boundaryNodes_receive) {
        this.siteBase.updateByNodes(boundaryNodes_receive);
        this.boundaryArea = siteBase.getBoundaryArea();
    }

    public void updateBoundary(Polygon boundary_receive) {
        siteBase.setBoundary(boundary_receive);
    }

    /* ------------- generating main traffic ------------- */

    /**
     * initialize main traffic
     *
     * @param bufferDist distance to buffer
     * @return void
     */
    public void initTraffic(double bufferDist) {
        this.mainTraffic = new MainTraffic(siteBase.getBoundary(), bufferDist);
        this.atriumRawManager = new AtriumRawManager(mainTraffic.getMainTrafficCurveWB());
    }

    /**
     * update main traffic
     *
     * @param innerNode_receive received inner node
     * @param entryNode_receive received entry node
     * @param bufferDist        distance to buffer
     * @return void
     */
    public void updateTraffic(List<WB_Point> entryNode_receive, List<WB_Point> innerNode_receive, double bufferDist) {
        mainTraffic.updateTraffic(entryNode_receive, innerNode_receive, bufferDist);
        atriumRawManager.updateAtriumRawByTraffic(mainTraffic.getMainTrafficCurveWB());
    }

    /**
     * update main traffic by raw nodes, set invalid node to proper position
     *
     * @param entryNode_receive received entry node
     * @param innerNode_receive received inner node
     * @param bufferDist        distance to buffer
     * @return void
     */
    public void updateTrafficByRawNodes(List<WB_Point> entryNode_receive, List<WB_Point> innerNode_receive, double bufferDist) {
        WB_Polygon boundTemp = ZTransform.PolygonToWB_Polygon(siteBase.getBoundary());
        for (WB_Point p : innerNode_receive) {
            if (!WB_GeometryOp.contains2D(p, boundTemp)) {
                WB_PolyLine pl = ZTransform.WB_PolygonToWB_PolyLine(boundTemp).get(0);
                WB_Point closest = WB_GeometryOp.getClosestPoint2D(p, pl);
                p.set(closest);
            }
        }
        for (WB_Point p : entryNode_receive) {
            WB_PolyLine pl = ZTransform.WB_PolygonToWB_PolyLine(boundTemp).get(0);
            WB_Point closest = WB_GeometryOp.getClosestPoint2D(p, pl);
            p.set(closest);
        }
        mainTraffic.updateTraffic(entryNode_receive, innerNode_receive, bufferDist);
    }

    /**
     * update main traffic width
     *
     * @param bufferDist distance to buffer
     * @return void
     */
    public void updateTrafficWidth(double bufferDist) {
        mainTraffic.updateTrafficWidth(bufferDist);
    }

    /* ------------- generating raw atrium ------------- */

    /**
     * add a raw atriumwang
     *
     * @param x          pointer x
     * @param y          pointer x
     * @param atriumType type id of atriumRaw
     * @return void
     */
    public void addAtriumRaw(double x, double y, int atriumType) {
        Polygon trafficBuffer = mainTraffic.getMainTrafficBuffer();
        Point p = ZFactory.jtsgf.createPoint(new Coordinate(x, y));
        WB_Point generator;
        if (trafficBuffer.contains(p)) {
            generator = ZTransform.PointToWB_Point(p);
        } else {
            WB_PolyLine bufferPL = ZTransform.PolygonToWB_PolyLine(trafficBuffer).get(0);
            WB_Point pWB = ZTransform.PointToWB_Point(p);
            generator = WB_GeometryOp.getClosestPoint2D(pWB, bufferPL);
        }
        if (generator != null) {
            switch (atriumType) {
                case 0:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumTri(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 1:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumSq(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 2:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumTra(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 3:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumPen(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 4:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumHex(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 5:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumHex2(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 6:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumLS(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
                case 7:
                    atriumRawManager.addAtriumRaw(AtriumRawFactory.createAtriumOct(generator, MallConst.ATRIUM_AREA_INIT, false));
                    break;
            }
        }
    }

    /**
     * remove raw atrium
     *
     * @param id index of updating raw atrium
     * @return void
     */
    public void removeAtriumRaw(int id) {
        atriumRawManager.removeAtriumRaw(id);
    }

    /**
     * change curve shape or polygon shape of the selected atrium
     *
     * @param id index of updating raw atrium
     * @return void
     */
    public void changeAtriumRawCurve(int id) {
        atriumRawManager.switchAtriumRawCurve(id);
    }

    /**
     * rotate raw atrium
     *
     * @param id    index of updating raw atrium
     * @param angle angle to rotate
     * @return void
     */
    public void rotateAtriumRaw(int id, double angle) {
        atriumRawManager.rotateAtriumRaw(id, angle);
    }

    /**
     * scale raw atrium by area
     *
     * @param id   index of updating raw atrium
     * @param area input area
     * @return void
     */
    public void changeAtriumRawArea(int id, double area) {
        atriumRawManager.changeAtriumRawArea(id, area);
    }

    /* ------------- generating main corridor & public space ------------- */

    /**
     * initialize the main corridor
     *
     * @param corridorWidth width of corridor
     * @return void
     */
    public void initMainCorridor(double corridorWidth) {
        this.publicSpace = new PublicSpace(rawAtrium_receive, mainTraffic.getMainTrafficCurve());
    }

    /**
     * update main corridor position
     *
     * @param corridorID id of changed corridor
     * @param newNodes   new corridor nodes
     * @return void
     */
    public void updateMainCorridorPos(int corridorID, WB_Point[] newNodes) {
        if (corridorID > -1) {
            publicSpace.updateCorridorPos(corridorID, new ZLine(newNodes[0], newNodes[1]));
        }
    }

    /**
     * update main corridor width
     *
     * @param corridorID id of changed corridor
     * @param width      new width
     * @return void
     */
    public void updateMainCorridorWidth(int corridorID, double width) {
        if (corridorID > -1) {
            publicSpace.updateCorridorWidth(corridorID, width);
        }
    }

    /*temp*/
    public void setPublicSpaceShapeTemp(Polygon publicSpaceShape) {
        this.publicSpace = new PublicSpace();
        this.publicSpace.setPublicSpaceShapeTemp(publicSpaceShape);
    }

    /**
     * initialize public traffic space
     *
     * @return void
     */
    public void initPublicSpaceShape() {
        publicSpace.initPublicShape();
    }

    /**
     * update public space by a new set of control points
     *
     * @param publicSpaceNode_interact new control points
     * @return void
     */
    public void updatePublicSpace(List<WB_Point> publicSpaceNode_interact) {
        Coordinate[] ctrls = new Coordinate[publicSpaceNode_interact.size()];
        for (int i = 0; i < ctrls.length; i++) {
            ctrls[i] = ZTransform.WB_CoordToCoordinate(publicSpaceNode_interact.get(i));
        }
        publicSpace.updatePublicShape(ctrls);
    }

    /**
     * update public space by buffer distance
     *
     * @param dist buffer distance
     * @return void
     */
    public void updatePublicSpaceBuffer(double dist) {
        publicSpace.updatePublicShapeBuffer(dist);
    }

    /**
     * switch between round mode and smooth mode
     *
     * @param atriumID index of the atrium to switch
     * @return void
     */
    public void switchAtriumRoundType(int atriumID) {
        publicSpace.switchAtriumRoundType(atriumID);
    }

    /**
     * update the radius of rounding atrium
     *
     * @param atriumID index of atrium
     * @param radius   new radius
     * @return void
     */
    public void updateAtriumRoundRadius(int atriumID, double radius) {
        publicSpace.updateAtriumRoundRadius(atriumID, radius);
    }

    /**
     * update smooth times of the atrium
     *
     * @param atriumID index of atrium
     * @param times    smooth count times
     * @return void
     */
    public void updateAtriumSmoothTimes(int atriumID, int times) {
        publicSpace.updateAtriumSmoothTimes(atriumID, times);
    }

    /* ------------- generating escalators ------------- */

    /**
     * initialize escalators in public space
     *
     * @return void
     */
    public void initEscalators() {
        publicSpace.initEscalators(mainTraffic.getMainTrafficCurve());
    }

    /**
     * update escalator position by counting ids
     *
     * @param atriumID the atrium id with selected escalator
     * @return void
     */
    public void updateEscalatorPos(int atriumID) {
        publicSpace.updateEscalatorPos(atriumID);
    }

    /* ------------- generating structure grid ------------- */

    /**
     * setup structure grids
     *
     * @param gridNum number of structure grids
     * @param dist    column distance
     * @return void
     */
    public void initGrid(int gridNum, double dist) {
        ZRectCover zrc = new ZRectCover(siteBase.getBoundary(), gridNum);
        List<Polygon> rects = zrc.getBestRects();
        this.grids = new StructureGrid[gridNum];
        for (int i = 0; i < rects.size(); i++) {
            grids[i] = new StructureGrid(rects.get(i), dist);
        }
    }

    /**
     * update structure grid given a new rectangle
     *
     * @param gridIndex index of changed grid
     * @param rect      new rectangle
     * @return void
     */
    public void updateGridByRect(int gridIndex, Polygon rect) {
        if (gridIndex > -1 && rect != null) {
            grids[gridIndex].updateRect(rect);
            Geometry union = grids[0].getRect();
            for (int i = 1; i < grids.length; i++) {
                union = union.union(grids[i].getRect());
            }
            union = union.buffer(1);
            this.validGrids = union.contains(siteBase.getBoundary());
        }
    }

    /**
     * switch grid model between 8.4m and 9m
     *
     * @return void
     */
    public void switchGridModulus() {
        this.gridModelSwitch = !gridModelSwitch;
        if (gridModelSwitch) {
            for (StructureGrid g : grids) {
                g.updateModel(MallConst.STRUCTURE_MODEL);
            }
        } else {
            for (StructureGrid g : grids) {
                g.updateModel(MallConst.STRUCTURE_MODEL_2);
            }
        }
    }

    /**
     * update structure grid modulus
     *
     * @param gridModulus modulus of the grid
     * @return void
     */
    public void updateGridModulus(double gridModulus) {
        for (StructureGrid g : grids) {
            g.updateModel(gridModulus);
        }
    }

    /* ------------- generating shop cells ------------- */

    /**
     * initialize shop cells
     *
     * @return void
     */
    public void initShopCells() {
        this.shopManager = new ShopManager();
        List<LineString> publicSpaceLS = new ArrayList<>(
                ZTransform.PolygonToLineString(publicSpace.getPublicSpaceShape())
        );

        shopManager.updateShopPartition(
                siteBase.getBoundary(),
                publicSpaceLS,
                publicSpace.getPublicSpaceShape().getInteriorPoint(),
                grids
        );
    }

    /**
     * split internal & external shops
     *
     * @param splitShopID selected id of shop to split
     * @return void
     */
    public void splitShopCell(List<Integer> splitShopID) {
        shopManager.updateSplit(splitShopID, publicSpace.getPublicSpaceShape(), grids);
    }

//    /**
//     * initialize shop cells
//     *
//     * @param floorNum number of floor
//     * @return void
//     */
//    public void initShopCells(int floorNum) {
//        if (floorNum == 1) {
//            floors[floorNum - 1] = new MallFloor(floorNum, siteBase.getBoundary());
//            floors[floorNum - 1].setStatus(0);
//            floors[floorNum - 1].updateSubdivision(publicSpace.getPublicSpaceShape(), grids);
//        } else {
//            // mainly here
//            floors[floorNum - 1] = new MallFloor(floorNum, siteBase.getBoundary());
//            floors[floorNum - 1].setStatus(0);
//            Point verify = publicSpace.getPublicSpaceShape().getInteriorPoint();
//            floors[floorNum - 1].setVerify(verify);
//            floors[floorNum - 1].updateSubdivision(publicSpace.getPublicSpaceShape(), grids);
//        }
//    }
//
//    /**
//     * split internal & external shops
//     *
//     * @param floorNum
//     * @param splitShopID
//     * @return void
//     */
//    public void splitShopCell(int floorNum, List<Integer> splitShopID) {
//        if (floorNum == 1) {
//
//        } else {
//            // mainly here
//            floors[floorNum - 1].updateSplit(splitShopID, publicSpace.getPublicSpaceShape(), grids);
//        }
//    }

    /* ------------- generating evacuations ------------- */

    /**
     * initialize evacuation stairway position
     *
     * @return void
     */
    public void initEvacStairwaysPos() {
        this.auxiliarySpace = new AuxiliarySpace();
        List<WB_Polygon> shopPolys = new ArrayList<>();
        for (Shop s : shopManager.getAllShops()) {
            WB_Polygon p = ZGeoMath.polygonFaceUp(s.getShapeWB());
            ZTransform.validateWB_Polygon(p);
            shopPolys.add(p);
        }

        double floorArea = boundaryArea;
        for (double a : publicSpace.getAtriumCurrAreas()) {
            floorArea -= a;
        }
        auxiliarySpace.initEvacuationGenerator(
                mainTraffic.getMainTrafficInnerWB(),
                shopPolys,
                floorArea,
                ZTransform.PolygonToWB_Polygon(siteBase.getBoundary())
        );
    }

    public void updateEvacPosByID(List<Integer> newEvacGenIDs) {
        auxiliarySpace.updateEvacuationGenerator(newEvacGenIDs);
    }

    public void generateEvacShape() {
        auxiliarySpace.generateStairwayShape(siteBase.getBoundary(), shopManager.getShopBlocks().get(0));
    }

    public void switchEvacDirection(int id) {
        auxiliarySpace.switchEvacDir(id, siteBase.getBoundary(), shopManager.getShopBlocks().get(0));
    }

    /* ------------- generating washrooms ------------- */

    public void initwashrooms() {
        auxiliarySpace.initWashroom(siteBase.getBoundary(), mainTraffic.getMainTrafficInnerLS());
    }

    /* ------------- setter & getter ------------- */

    public SiteBase getSiteBase() {
        return siteBase;
    }

    public String getBoundaryAreaAsString() {
        return boundaryArea > 0 ? String.format("%.2f", boundaryArea) + "㎡" : "";
    }

    public MainTraffic getMainTraffic() {
        return mainTraffic;
    }

    public AtriumRawManager getAtriumRawManager() {
        return atriumRawManager;
    }

    public List<Polygon> getAtriumRawShapes() {
        List<Polygon> polygons = new ArrayList<>();
        for (AtriumRaw a : atriumRawManager.getAtriumRaws()) {
            polygons.add(a.getShape());
        }
        return polygons;
    }

    public WB_Point getChangedAtriumRawCenter(int id) {
        return atriumRawManager.getAtriumRaws().get(id).getCenter();
    }

    public WB_Point[] getChangedAtriumRawShpts(int id) {
        return atriumRawManager.getAtriumRaws().get(id).getShapePoints();

    }

    public void setRawAtrium_receive(Polygon[] rawAtrium_receive) {
        this.rawAtrium_receive = rawAtrium_receive;
    }

    public PublicSpace getPublicSpace() {
        return publicSpace;
    }

    public List<WB_Point> getCorridorNode() {
        List<WB_Point> corridorNode = new ArrayList<>();
        for (ZLine l : publicSpace.getCorridorsLines()) {
            corridorNode.add(l.getPt0().toWB_Point());
            corridorNode.add(l.getPt1().toWB_Point());
        }
        return corridorNode;
    }

    public List<Polygon> getEscalatorBounds() {
        return publicSpace.getEscalatorBounds();
    }

    public List<Integer> getEscalatorAtriumIDs() {
        return publicSpace.getEscalatorAtriumIDs();
    }

    public Polygon getEscalatorBoundN(int escalatorAtriumID) {
        return publicSpace.getEscalatorBoundN(escalatorAtriumID);
    }

    public StructureGrid[] getGrids() {
        return grids;
    }

    public Polygon[] getGridRects() {
        Polygon[] rects = new Polygon[grids.length];
        for (int i = 0; i < grids.length; i++) {
            rects[i] = grids[i].getRect();
        }
        return rects;
    }

    public List<Shop> getShopCells() {
        return shopManager.getAllShops();
    }

    public void setShopCells(List<Polygon> shopCellPolys) {
        List<Shop> newShops = new ArrayList<>();
        for (Polygon p : shopCellPolys) {
            newShops.add(new Shop(p));
        }
        shopManager.setAllShops(newShops);
    }

    public AuxiliarySpace getAuxiliarySpace() {
        return auxiliarySpace;
    }

    /* ------------- draw ------------- */

    public void displayLocal(PApplet app, WB_Render render, JtsRender jtsRender, int status) {
        app.pushStyle();
        switch (status) {
            case -1:
                break;
            case MallConst.E_SITE_BOUNDARY:
                displaySiteBoundaryLocal(app, jtsRender);
                break;
            case MallConst.E_TRAFFIC_ATRIUM:
                displaySiteBoundaryLocal(app, jtsRender);
                displayTrafficLocal(app, jtsRender);
                break;
            case MallConst.E_MAIN_CORRIDOR:
                displaySiteBoundaryLocal(app, jtsRender);
                displayTrafficLocal(app, jtsRender);
                displayMainCorridorLocal(app, jtsRender);
                break;
            case MallConst.E_PUBLIC_SPACE:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                break;
            case MallConst.E_ESCALATOR:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                displayEscalatorLocal(app, jtsRender);
                displayEscalatorRadiusLocal(app);
                break;
            case MallConst.E_STRUCTURE_GRID:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                displayEscalatorLocal(app, jtsRender);
                displayGridLocal(app);
                break;
            case MallConst.E_SHOP_EDIT:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                displayEscalatorLocal(app, jtsRender);
                displayGridLocal(app);
                displayShopCellsLocal(app, jtsRender);
                break;
            case MallConst.E_EVAC_STAIRWAY:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                displayEscalatorLocal(app, jtsRender);
                displayShopCellsLocal(app, jtsRender);
                displayGridLocal(app);
                displayEvacStairwayPosLocal(app);
                displayEvacStairwayLocal(app, jtsRender);
                break;
            case MallConst.E_WASHROOM:
                displaySiteBoundaryLocal(app, jtsRender);
                displayPublicSpaceLocal(app, jtsRender);
                displayEscalatorLocal(app, jtsRender);
                displayShopCellsLocal(app, jtsRender);
                displayGridLocal(app);
                displayEvacStairwayPosLocal(app);
                break;
        }
        app.popStyle();
    }

    public void displaySiteBoundaryLocal(PApplet app, JtsRender render) {
        // draw boundary and site
        app.noFill();
        app.stroke(255);
        app.strokeWeight(6);
        render.drawGeometry(siteBase.getBoundary());
        app.stroke(255, 0, 0);
        app.strokeWeight(3);
        render.drawGeometry(siteBase.getSite());
    }

    public void displayTrafficLocal(PApplet app, JtsRender jtsRender) {
        // draw traffic route and buffer area
        app.stroke(255);
        app.strokeWeight(1);
        jtsRender.drawGeometry(mainTraffic.getMainTrafficCurve());
        app.stroke(52, 170, 187);
        app.strokeWeight(3);
        jtsRender.drawGeometry(mainTraffic.getMainTrafficBuffer());
    }

    public void displayMainCorridorLocal(PApplet app, JtsRender jtsRender) {
        app.strokeWeight(3);
        app.stroke(55, 103, 171);
        app.noFill();
        for (Polygon a : publicSpace.getAtriumTrimShapes()) {
            jtsRender.drawGeometry(a);
        }

        app.strokeWeight(1);
        app.stroke(0, 255, 0);
        for (int i = 0; i < publicSpace.getCorridorsLines().size(); i++) {
            publicSpace.getCorridorsLines().get(i).display(app);
        }

        app.fill(255);
        app.textSize(2);
        for (int i = 0; i < publicSpace.getAtriumTrimShapes().length; i++) {
            Polygon p = publicSpace.getAtriumTrimShapes()[i];
            app.pushMatrix();
            app.scale(1, -1);
            app.translate(0, (float) (-2 * p.getCentroid().getY()));
            app.text(String.format("%.2f", publicSpace.getAtriumCurrAreas()[i]), (float) p.getCentroid().getX(), (float) p.getCentroid().getY());
            app.popMatrix();
        }
    }

    public void displayPublicSpaceLocal(PApplet app, JtsRender jtsRender) {
        app.noFill();
        app.stroke(52, 170, 187);
        app.strokeWeight(3);
        jtsRender.drawGeometry(publicSpace.getPublicSpaceShape());
        for (Polygon p : publicSpace.getAtriumCurrShapes()) {
            jtsRender.drawGeometry(p);
        }

        app.fill(255);
        app.textSize(2);
        for (int i = 0; i < publicSpace.getAtriumTrimShapes().length; i++) {
            Polygon p = publicSpace.getAtriumTrimShapes()[i];
            app.pushMatrix();
            app.scale(1, -1);
            app.translate(0, (float) (-2 * p.getCentroid().getY()));
            app.text(String.format("%.2f", publicSpace.getAtriumCurrAreas()[i]), (float) p.getCentroid().getX(), (float) p.getCentroid().getY());
            app.popMatrix();
        }
    }

    public void displayGridLocal(PApplet app) {
        app.strokeWeight(0.5f);
        if (validGrids) {
            app.stroke(128);
        } else {
            app.stroke(255, 0, 0);
        }

        for (StructureGrid grid : grids) {
            for (ZLine l : grid.getAllLines()) {
                l.display(app);
            }
        }
    }

    public void displayEscalatorLocal(PApplet app, JtsRender jtsRender) {
        app.noFill();
        app.stroke(255);
        app.strokeWeight(1);
        for (Polygon p : publicSpace.getEscalatorBounds()) {
            jtsRender.drawGeometry(p);
        }
        for (ZLine l : publicSpace.getEscalatorShapes()) {
            l.display(app);
        }
    }

    public void displayEscalatorRadiusLocal(PApplet app) {
        for (Polygon p : publicSpace.getEscalatorBounds()) {
            Point centroid = p.getCentroid();
            app.noFill();
            app.ellipse((float) centroid.getX(), (float) centroid.getY(), 2 * MallConst.ESCALATOR_DIST_MIN, 2 * MallConst.ESCALATOR_DIST_MIN);
            app.line(
                    (float) centroid.getX(),
                    (float) centroid.getY(),
                    (float) centroid.getX() + MallConst.ESCALATOR_DIST_MIN,
                    (float) centroid.getY()
            );
            app.fill(255);
            app.textSize(2);
            app.pushMatrix();
            app.scale(1, -1);
            app.translate(0, (float) (-2 * centroid.getY()));
            app.text("30m",
                    (float) centroid.getX() + 0.5f * MallConst.ESCALATOR_DIST_MIN,
                    (float) centroid.getY() + 1
            );
            app.popMatrix();
        }
    }

    public void displayShopCellsLocal(PApplet app, JtsRender jtsRender) {
        if (shopManager.getAllShops() != null) {
            app.strokeWeight(3);
            app.stroke(255);
            List<Shop> cells = shopManager.getAllShops();
            for (Shop s : cells) {
                jtsRender.drawGeometry(s.getShape());
            }
            app.noStroke();
            for (Shop s : shopManager.getAllShops()) {
                s.display(app, jtsRender);
            }

            app.fill(255);
            app.textSize(2);

            for (Shop s : shopManager.getAllShops()) {
                app.pushMatrix();
                app.scale(1, -1);
                app.translate(0, (float) (-2 * s.getCenter().getY()));
                s.displayText(app);
                app.popMatrix();
            }
        }
    }

    public void displayEvacStairwayPosLocal(PApplet app) {
        app.strokeWeight(3);
        app.stroke(255, 97, 136);
        app.noFill();
        for (ZLine path : auxiliarySpace.getCoveredPath()) {
            ZRender.drawZLine2D(app, path);
        }

//        app.noStroke();
//        app.fill(255, 97, 136);
//        for (ZPoint generator : auxiliarySpace.getSelGeneratorPos()) {
//            ZRender.drawZPoint(app, generator, 5);
//        }
    }

    public void displayEvacStairwayLocal(PApplet app, JtsRender jtsRender) {
        if (auxiliarySpace.getEvacShapes() != null) {
            app.pushMatrix();
            app.translate(0, 0, 0.5f);
            app.strokeWeight(3);
            app.stroke(255);
            app.fill(82);
            for (Polygon shape : auxiliarySpace.getStairwayShapePoly()) {
                jtsRender.drawGeometry(shape);
            }
            app.popMatrix();
        }
    }
}