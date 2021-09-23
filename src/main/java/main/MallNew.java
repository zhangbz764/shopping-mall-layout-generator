package main;

import controlP5.ControlEvent;
import controlP5.ControlP5;
import guo_cam.CameraController;
import processing.core.PApplet;
import processing.core.PFont;
import render.JtsRender;
import transform.ZTransform;
import wblut.processing.WB_Render;

/**
 * main PApplet entry for shopping mall
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2021/5/9
 * @time 16:21
 */
public class MallNew extends PApplet {
    // utils
    private CameraController gcam;
    private WB_Render render;
    private JtsRender jtsRender;
    private ControlP5 cp5;
    private int cp5H;

    // pointer from screen
    private double[] pointer;

    // main
    private final ImportData input = new ImportData();
    private MallGenerator mallGenerator;
    private MallGUI mallGUI;
    private MallInteract mallInteract;
    private MallParam mallParam;

    // edit switches
    public int EDIT_STATUS = -1;

    // stats
    public int FLOOR_NUM = 2;

    /* ------------- settings ------------- */

    public void settings() {
        size(1600, 900, P3D);
    }

    /* ------------- setup ------------- */

    public void setup() {
        // utils
        this.gcam = new CameraController(this);
        gcam.top();
        this.render = new WB_Render(this);
        this.jtsRender = new JtsRender(this);

        PFont font = createFont("./src/main/resources/simhei.ttf", 32);
        textFont(font);
        textAlign(CENTER, CENTER);
        textSize(14);

        // import
        String importPath = "./src/main/resources/0310.3dm";
        this.input.loadData(importPath);

        // initialize core managers
        this.mallInteract = new MallInteract();
        this.mallGenerator = new MallGenerator();
        this.mallGUI = new MallGUI(font);
        this.mallParam = new MallParam();

        // GUI
        this.cp5 = new ControlP5(this);
        this.cp5H = (int) (height * 0.5 / MallConst.STATUS_NUM);
        mallGUI.initGUI(cp5, cp5H, mallParam);
    }

    /* ------------- draw ------------- */

    public void draw() {
        background(33, 40, 48);

        // main
        gcam.begin3d();
        mallGenerator.displayLocal(this, render, jtsRender, EDIT_STATUS, FLOOR_NUM);
        mallInteract.displayLocal(this, render, jtsRender, EDIT_STATUS);

        // stats and info
        gcam.begin2d();
        pushStyle();
        showGUI();
        displayStats();
        mallGUI.displayInfo(this, EDIT_STATUS);
        popStyle();
    }

    /**
     * display GUI
     */
    public void showGUI() {
        // background rectangle
        noStroke();
        fill(55, 103, 171, 100);
        rect(0, 0, MallConst.STATUS_W, height);

        // select rectangle
        if (EDIT_STATUS > -1) {
            noFill();
            stroke(83, 192, 231);
            strokeWeight(3);
            rect(0, cp5H * EDIT_STATUS, MallConst.STATUS_W, cp5H);
        }

        // info right-top

    }

    /**
     * display shopping mall statistics
     */
    public void displayStats() {
        textAlign(LEFT);

        fill(255);
        String stats = "本层面积" + "\n" + mallGenerator.getBoundaryAreaAsString() + "\n"
                + "\n" + "可租赁面积" + "\n" + mallInteract.getRentArea() + "\n"
                + "\n" + "动线长度" + "\n" + mallGenerator.getTrafficLength() + "\n" + mallGenerator.getInnerTrafficLength() + "\n"
                + "\n" + "得铺率" + "\n" + mallInteract.getShopRatio() + "\n"
                + "\n" + "小铺率" + "\n" + mallInteract.getSmallShopRatio() + "\n";
        text(stats, 20, height * 0.5f + 50);
    }

    /* ------------- interact ------------- */

    /**
     * key interact
     *
     * @return void
     */
    public void keyPressed() {
//        // initialize
//        if (key == '`') {
//            mallGenerator.setBufferCurve_receive(FLOOR_NUM, mallInteract.getBufferCurve_interact());
//            mallGenerator.generateEvacuation2();
//            mallGenerator.generateSubdivision(FLOOR_NUM);
//
//            this.currFloorStats = mallGenerator.getFloorStats(FLOOR_NUM);
//        }
//        if (key == '.') {
//            mallInteract.clickUpdateShop();
//            mallGenerator.setShopCells_receive(FLOOR_NUM, mallInteract.getCellPolys_interact());
//        }

        // floor switches
        if (key == '=') {
            FLOOR_NUM = (FLOOR_NUM % MallConst.FLOOR_TOTAL) + 1;
//            generatorGraphBuffer();
            println("current floor: " + FLOOR_NUM);
        }
        if (key == '-') {
            if (FLOOR_NUM == 1) {
                FLOOR_NUM = MallConst.FLOOR_TOTAL;
            } else {
                FLOOR_NUM--;
            }
//            generatorGraphBuffer();
            println("current floor: " + FLOOR_NUM);
        }

        // add & remove control points
//        if (key == 'q' || key == 'Q') {
//            pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);
//            mallInteract.addInnerNode(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
//        }
//        if (key == 'w' || key == 'W') {
//            pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);
//            mallInteract.removeInnerNode(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
//        }
//        if (key == 'e' || key == 'E') {
//            pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);
//            mallInteract.addEntryNode(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
//        }
//        if (key == 'r' || key == 'R') {
//            pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);
//            mallInteract.removeEntryNode(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
//        }


        if (key == '3') {
            println("edit curve shape");
        }
//        if (key == '4') {
//            mallInteract.setCellPolys_interact(mallGenerator.getShopCells(FLOOR_NUM));
//            println("edit shop cells");
//        }


//        if (key == 's') {
//            sl.saveEdit(EDIT_STATUS, mallInteract, mallGenerator);
//        }
//        if (key == 'l') {
//            List data = sl.loadEdit();
//            if (data.size() == 3) {
//                EDIT_STATUS = (int) data.get(0);
//                mallInteract = (MallInteract) data.get(1);
//                mallGenerator = (MallGenerator) data.get(2);
//                mallGUI.updateGUI(EDIT_STATUS, cp5, cp5H);
//            } else {
//                System.out.println("?");
//            }
//        }
    }

    /**
     * mouse drag interact
     *
     * @return void
     */
    public void mouseDragged() {
        if (mouseButton == LEFT) {
            pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);

            switch (EDIT_STATUS) {
                case MallConst.E_SITE_BOUNDARY:
                    mallInteract.dragUpdateBoundary(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                    break;
                case MallConst.E_TRAFFIC_ATRIUM:
                    mallInteract.dragUpdateTrafficAtriumRaw(pointer[0] + width * 0.5, pointer[1] + height * 0.5, mallGenerator.getBoundary());
                    break;
                case MallConst.E_MAIN_CORRIDOR:
                    mallInteract.dragUpdateCorridor(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                    break;
                case MallConst.E_STRUCTURE_GRID:
                    mallInteract.dragUpdateGrid(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                    break;
            }
        }
    }

    /**
     * mouse release interact
     *
     * @return void
     */
    public void mouseReleased() {
        if (mouseButton == LEFT) {
            if (mouseX > MallConst.CONTROLLER_W + MallConst.STATUS_W) {  // 不碰到GUI
                switch (EDIT_STATUS) {
                    case MallConst.E_SITE_BOUNDARY:
                        mallGenerator.setBoundary(mallInteract.getBoundary_controllers());
                        break;
                    case MallConst.E_TRAFFIC_ATRIUM:
                        if (mallInteract.getTrafficOrAtrium()) {
                            mallGenerator.updateTraffic(
                                    mallInteract.getTraffic_innerControllers(),
                                    mallInteract.getTraffic_entryControllers(),
                                    mallParam.trafficBufferDist
                            );
                            mallInteract.getAtriumRawManager().updateAtriumRawByTraffic(
                                    ZTransform.LineStringToWB_PolyLine(mallGenerator.getMainTrafficCurve())
                            );
                            mallInteract.getAtriumRawManager().validateAtriumRaw();
                            mallInteract.setMainTraffic_buffer(mallGenerator.getMainTrafficBuffer());
                        } else {
                            mallInteract.releaseUpdateAtriumRaw();
                        }
                        break;
                    case MallConst.E_MAIN_CORRIDOR:
                        mallGenerator.updateMainCorridorPos(mallInteract.getSelectedCorridorID(), mallInteract.getSelectedCorridorNode());
                        break;
                    case MallConst.E_STRUCTURE_GRID:
                        mallGenerator.updateGridByRect(mallInteract.getSelectedRectID(), mallInteract.getSelectedRect());
                        break;
                }
            }
        }
    }

    /**
     * mouse click interact
     *
     * @return void
     */
    public void mouseClicked() {
        if (mouseButton == LEFT) {
            if (mouseX > MallConst.CONTROLLER_W + MallConst.STATUS_W) {  // 不碰到GUI
                pointer = gcam.getCoordinateFromScreenDouble(mouseX, mouseY, 0);

                switch (EDIT_STATUS) {
                    case MallConst.E_TRAFFIC_ATRIUM:
                        mallInteract.clickUpdateAtriumRaw(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                        break;
                    case MallConst.E_MAIN_CORRIDOR:
                        mallInteract.selectCorridorNode(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                        break;
                    case MallConst.E_PUBLIC_SPACE:
                        mallInteract.selectPublicSpaceNodeOrAtrium(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                        break;
                    case MallConst.E_STRUCTURE_GRID:
                        mallInteract.selectGridRect(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                        break;
                    case MallConst.E_SHOP_EDIT:
                        mallInteract.selectShopCell(pointer[0] + width * 0.5, pointer[1] + height * 0.5);
                        break;
                }
            }
        }
    }

    /**
     * controlP5 event
     *
     * @param theEvent ControlEvent
     * @return void
     */
    public void controlEvent(ControlEvent theEvent) {
        int id = theEvent.getController().getId();

        // edit status events
        switch (id) {
            case (MallConst.E_SITE_BOUNDARY):
                if (EDIT_STATUS >= MallConst.E_SITE_BOUNDARY - 1) {
                    // 编辑外轮廓
                    if (EDIT_STATUS <= MallConst.E_SITE_BOUNDARY) {
                        // if initializing
                        mallGenerator.initSiteBoundary(
                                input.getInputSite(),
                                input.getInputBoundary(),
                                0,
                                mallParam.siteRedLineDist,
                                mallParam.siteBufferDist
                        );
                    } else {
                        mallInteract.clearActiveEdit(EDIT_STATUS);
                    }
                    mallInteract.setBoundary_controllers(mallGenerator.getBoundaryNodes());

                    this.EDIT_STATUS = MallConst.E_SITE_BOUNDARY;
                    mallGUI.updateGUI(EDIT_STATUS, cp5);
                    println("edit boundary");
                }
                break;
            case (MallConst.E_TRAFFIC_ATRIUM):
                if (EDIT_STATUS >= MallConst.E_TRAFFIC_ATRIUM - 1) {
                    if (EDIT_STATUS <= MallConst.E_TRAFFIC_ATRIUM) {
                        // 外轮廓不可编辑，编辑主路径
                        mallGenerator.setBoundary(mallInteract.getBoundary_controllers());
                        mallGenerator.initTraffic(mallParam.trafficBufferDist);

                        mallInteract.setTraffic_innerControllers(mallGenerator.getTrafficInnerNodes());
                        mallInteract.setTraffic_entryControllers(mallGenerator.getTrafficEntryNodes());

                        mallInteract.initAtriumRawManager(ZTransform.LineStringToWB_PolyLine(mallGenerator.getMainTrafficCurve()));
                        mallInteract.setMainTraffic_buffer(mallGenerator.getMainTrafficBuffer());
                    } else {
                        mallInteract.clearActiveEdit(EDIT_STATUS);
                    }
                    this.EDIT_STATUS = MallConst.E_TRAFFIC_ATRIUM;
                    mallGUI.updateGUI(EDIT_STATUS, cp5);
                    println("edit traffic and raw atrium");
                }
                break;
            case (MallConst.E_MAIN_CORRIDOR):
                if (EDIT_STATUS >= MallConst.E_MAIN_CORRIDOR - 1) {
                    // make sure that raw atriums are valid
                    if (mallInteract.getAtriumRawManager().getValidAtriumRaw()) {
                        if (EDIT_STATUS <= MallConst.E_MAIN_CORRIDOR) {
                            mallGenerator.setRawAtrium_receive(mallInteract.getAtriumRawShapes());
                            mallGenerator.initMainCorridor(mallParam.corridorWidth);
                            mallInteract.setCorridorNode_interact(mallGenerator.getCorridorNode());
                        } else {
                            mallInteract.clearActiveEdit(EDIT_STATUS);
                        }

                        this.EDIT_STATUS = MallConst.E_MAIN_CORRIDOR;
                        mallGUI.updateGUI(EDIT_STATUS, cp5);
                        println("edit main corridor");
                    }
                }
                break;
            case (MallConst.E_PUBLIC_SPACE):
                if (EDIT_STATUS >= MallConst.E_PUBLIC_SPACE - 1) {
                    if (EDIT_STATUS <= MallConst.E_PUBLIC_SPACE) {
                        mallGenerator.initPublicSpace();
                        mallInteract.setPublicSpaceNode_interact(mallGenerator.getPublicSpaceCurveCtrls());
                        mallInteract.setAtrium_interact(mallGenerator.getAtriumCurrShapes());
                    } else {

                    }

                    this.EDIT_STATUS = MallConst.E_PUBLIC_SPACE;
                    mallGUI.updateGUI(EDIT_STATUS, cp5);
                    println("edit public space");
                }
                break;
            case (MallConst.E_STRUCTURE_GRID):
                if (EDIT_STATUS >= MallConst.E_STRUCTURE_GRID - 1) {
                    mallGenerator.initGrid(MallConst.STRUCTURE_GRID_NUM, MallConst.STRUCTURE_MODEL);
                    mallInteract.setRect_interact(mallGenerator.getGridRects());

                    this.EDIT_STATUS = MallConst.E_STRUCTURE_GRID;
                    mallGUI.updateGUI(EDIT_STATUS, cp5);
                    println("edit structure grid");
                }
                break;
            case (MallConst.E_SHOP_EDIT):
                if (EDIT_STATUS >= MallConst.E_SHOP_EDIT - 1) {
                    mallGenerator.initShopCells(FLOOR_NUM);
                    mallInteract.setShopCell_interact(mallGenerator.getShopCells(FLOOR_NUM));

                    this.EDIT_STATUS = MallConst.E_SHOP_EDIT;
                    mallGUI.updateGUI(EDIT_STATUS, cp5);
                    println("edit shop cells");
                }
                break;
            case (MallConst.E_ESCALATOR):
                if (EDIT_STATUS >= MallConst.E_ESCALATOR - 1) {

                }
                break;
            case (MallConst.E_EVACUATION):
                if (EDIT_STATUS >= MallConst.E_EVACUATION - 1) {

                }
                break;
        }

        // events in each edit status
        switch (EDIT_STATUS) {
            case (MallConst.E_SITE_BOUNDARY):
                switch (id) {
                    // 0
                    case (MallConst.BUTTON_SWITCH_BOUNDARY):
                        mallInteract.switchBoundary(mallParam.siteBufferDist);
                        mallGenerator.updateSiteBoundary(
                                mallInteract.getBoundaryBase(),
                                mallParam.siteRedLineDist,
                                mallParam.siteBufferDist
                        );
                        mallInteract.setBoundary_controllers(mallGenerator.getBoundaryNodes());
                        break;
                    case (MallConst.SLIDER_REDLINE_DIST):
                    case (MallConst.SLIDER_SITE_BUFFER):
                        mallGenerator.updateSiteBoundary(
                                mallInteract.getBoundaryBase(),
                                mallParam.siteRedLineDist,
                                mallParam.siteBufferDist
                        );
                        mallInteract.setBoundary_controllers(mallGenerator.getBoundaryNodes());
                        break;
                }
                break;
            case (MallConst.E_TRAFFIC_ATRIUM):
                switch (id) {
                    // 1
//                    case (MallConst.BUTTON_DELETE_INNERNODE):
//                        break;
//                    case (MallConst.BUTTON_DELETE_ENTRYNODE):
//                        break;
                    case (MallConst.BUTTON_TRAFFIC_CONTROLLERS):
                        mallInteract.reverseTrafficOrAtriumRaw();
                        break;
                    case (MallConst.SLIDER_TRAFFIC_WIDTH):
                        if (mallInteract.getTrafficOrAtrium()) {
                            mallGenerator.updateTrafficWidth(mallParam.trafficBufferDist);
                            mallInteract.setMainTraffic_buffer(mallGenerator.getMainTrafficBuffer());
                        }
                        break;
                    case (MallConst.BUTTON_CURVE_ATRIUM):
                        mallInteract.changeAtriumRawCurve();
                        break;
                    case (MallConst.BUTTON_DELETE_ATRIUM):
                        mallInteract.removeAtriumRaw();
                        break;
                    case (MallConst.SLIDER_ATRIUM_ANGLE):
                        float angle = mallParam.atriumAngle;
                        double anglePI = 2 * Math.PI * (angle / 360);
                        mallInteract.rotateAtriumRaw(anglePI);
                        break;
                    case (MallConst.SLIDER_ATRIUM_AREA):
                        mallInteract.changeAtriumRawArea(mallParam.atriumArea);
                        break;
                    case (MallConst.LIST_ATRIUM_FACTORY):
                        int atriumTypeNum = (int) theEvent.getController().getValue();
                        mallInteract.setSelectedAtriumRawType(atriumTypeNum);
                        break;
                }
                break;
            case (MallConst.E_MAIN_CORRIDOR):
                switch (id) {
                    // 2
                    case (MallConst.SLIDER_CORRIDOR_WIDTH):
                        mallGenerator.updateMainCorridorWidth(mallInteract.getSelectedCorridorID(), mallParam.corridorWidth);
                        break;
                }
                break;
            case (MallConst.E_PUBLIC_SPACE):
                int atriumID = mallInteract.getSelectedAtriumID();
                switch (id) {
                    // 3
                    case (MallConst.BUTTON_DELETE_PUBLIC_NODE):
                        mallInteract.removePublicSpaceNode();
                        mallGenerator.updatePublicSpace(mallInteract.getPublicSpaceNode_interact());
                        break;
                    case (MallConst.SLIDER_BUFFER_DIST):
                        mallGenerator.updatePublicSpaceBuffer(mallParam.publicSpaceBufferDist);
                        mallInteract.setPublicSpaceNode_interact(mallGenerator.getPublicSpaceCurveCtrls());
                        break;
                    case (MallConst.BUTTON_ATRIUM_ROUND):
                        if (atriumID > -1) {
                            mallGenerator.switchAtriumRoundType(atriumID);
                            mallInteract.setChangedAtrium(mallGenerator.getAtriumCurrShape(atriumID), atriumID);
                        }
                        break;
                    case (MallConst.SLIDER_ROUND_RADIUS):
                        if (atriumID > -1) {
                            mallGenerator.updateAtriumRoundRadius(atriumID, mallParam.atriumRoundRadius);
                            mallInteract.setChangedAtrium(mallGenerator.getAtriumCurrShape(atriumID), atriumID);
                        }
                        break;
                    case (MallConst.SLIDER_SMOOTH_TIMES):
                        if (atriumID > -1) {
                            mallGenerator.updateAtriumSmoothTimes(atriumID, mallParam.atriumSmoothTimes);
                            mallInteract.setChangedAtrium(mallGenerator.getAtriumCurrShape(atriumID), atriumID);
                        }
                        break;
                }
                break;
            case (MallConst.E_STRUCTURE_GRID):
                switch (id) {
                    // 4
                    case (MallConst.BUTTON_GRID_MODEL):
                        mallGenerator.switchGridModel();
                        break;
                    case (MallConst.LIST_GRID_NUM):
                        int gridNum = (int) theEvent.getController().getValue() + 1;
                        mallGenerator.initGrid(gridNum, MallConst.STRUCTURE_MODEL);
                        mallInteract.setRect_interact(mallGenerator.getGridRects());
                        mallInteract.unselectGridRect();
                        break;
                }
                break;
            case (MallConst.E_SHOP_EDIT):
                switch (id) {
                    // 5
                    case (MallConst.BUTTON_UNION_CELLS):
                        mallInteract.unionShopCell();
                        mallGenerator.setShopCells(FLOOR_NUM, mallInteract.getShopCell_interact());
                        break;
                }
                break;

            case (MallConst.E_ESCALATOR):
                break;
            case (MallConst.E_EVACUATION):
                break;
        }
    }
}
