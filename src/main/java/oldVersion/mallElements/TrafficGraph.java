package oldVersion.mallElements;

import basicGeometry.ZEdge;
import basicGeometry.ZFactory;
import basicGeometry.ZGraph;
import basicGeometry.ZPoint;
import math.ZMath;
import oldVersion.MallConstant;
import org.locationtech.jts.geom.LineString;
import processing.core.PApplet;
import render.ZRender;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Segment;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;


/**
 * generate minimum spanning tree from a series of input ZNodes
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/11
 * @time 14:59
 */
public class TrafficGraph {
    private static final float MAXVALUE = Float.MAX_VALUE;
    public boolean update = false;

    // adjacency matrix
    private double[][] matrix;
    // nodes and edges
    private final List<TrafficNode> fixedNodes, treeNodes;
    private List<ZEdge> fixedEdges, treeEdges;
    private TrafficNode selectedNode;

    /* ------------- constructor ------------- */

    public TrafficGraph() {
        this.treeNodes = new ArrayList<>();
        this.fixedNodes = new ArrayList<>();
        this.treeEdges = new ArrayList<>();
        this.fixedEdges = new ArrayList<>();
    }

    public TrafficGraph(List<TrafficNode> treeNodes, List<TrafficNode> fixedNodes) {
        this.treeNodes = treeNodes;
        this.fixedNodes = fixedNodes;
        init();
    }

    public TrafficGraph(List<TrafficNode> treeNodes) {
        this.treeNodes = treeNodes;
        this.fixedNodes = new ArrayList<>();
        init();
    }

    /* ------------- initialize & get (public) ------------- */

    /**
     * initialize adjacency matrix and tree, including fixed nodes if necessary
     *
     * @return void
     */
    public void init() {
        update = true; // changed
        if (treeNodes.size() > 0) {
            if (fixedNodes.size() > 0) {
                getFixedLink();
            } else {
                this.fixedEdges = new ArrayList<>();
            }
            if (treeNodes.size() > 1) {
                matrixInit();
                getTree();
            } else {
                this.treeEdges = new ArrayList<>();
            }
        } else {
            this.treeEdges = new ArrayList<>();
            this.fixedEdges = new ArrayList<>();
        }
        setRelations();
    }

    public List<TrafficNode> getTreeNodes() {
        return treeNodes;
    }

    public List<TrafficNode> getFixedNodes() {
        return fixedNodes;
    }

    public List<TrafficNode> getAllNodes() {
        List<TrafficNode> allNodes = fixedNodes;
        allNodes.addAll(treeNodes);
        return allNodes;
    }

    public List<ZEdge> getTreeEdges() {
        return treeEdges;
    }

    public List<ZEdge> getFixedEdges() {
        return fixedEdges;
    }

    public List<ZEdge> getAllEdges() {
        List<ZEdge> allEdges = treeEdges;
        allEdges.addAll(fixedEdges);
        return allEdges;
    }

    /**
     * clear all fixed nodes
     *
     * @return void
     */
    public void clearFixed() {
        for (TrafficNode fixed : fixedNodes) {
            fixed.getNeighbors().get(0).removeNeighbor(fixed);
        }
        this.fixedNodes.clear();
        this.fixedEdges.clear();
    }

    public ZGraph duplicate() {
        return ZFactory.createZGraphFromSegments(getAllEdges());
    }

    /**
     * transform all edges to a list of WB_Segment
     *
     * @return java.util.List<org.locationtech.jts.geom.LineString>
     */
    public List<WB_Segment> toWB_Segments() {
        List<WB_Segment> segments = new ArrayList<>();
        for (ZEdge te : treeEdges) {
            segments.add(te.toWB_Segment());
        }
        for (ZEdge fe : fixedEdges) {
            segments.add(fe.toWB_Segment());
        }
        return segments;
    }

    /**
     * transform all edges to a list of LineStrings
     *
     * @return java.util.List<org.locationtech.jts.geom.LineString>
     */
    public List<LineString> toLineStrings() {
        List<LineString> ls = new ArrayList<>();
        for (ZEdge te : treeEdges) {
            ls.add(te.toJtsLineString());
        }
        for (ZEdge fe : fixedEdges) {
            ls.add(fe.toJtsLineString());
        }
        return ls;
    }

    /* ------------- move, add & remove (public) ------------- */

    /**
     * reset all nodes' status
     *
     * @return void
     */
    public void resetActive() {
        for (TrafficNode n : fixedNodes) {
            n.setActive(false);
        }
        for (TrafficNode n : treeNodes) {
            n.setActive(false);
        }
    }

    /**
     * set a tree node by restriction (should be within the boundary polygon)
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void setTreeNode(int pointerX, int pointerY) {
        for (TrafficNode n : treeNodes) {
            if (n.isMoused(pointerX, pointerY)) {
                n.setActive(true);
                n.setLastPosition(n.xd(), n.yd(), n.zd());
                n.setByRestriction(pointerX, pointerY);
                init();
                break;
            }
            if (n.isActive()) {
                n.setLastPosition(n.xd(), n.yd(), n.zd());
                n.setByRestriction(pointerX, pointerY);
                init();
                break;
            }
        }
    }

    /**
     * set a tree node by restriction (should be on the boundary edges)
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void setFixedNode(int pointerX, int pointerY) {
        for (TrafficNode n : fixedNodes) {
            if (n.isMoused(pointerX, pointerY)) {
                n.setActive(true);
                n.setLastPosition(n.xd(), n.yd(), n.zd());
                n.setByRestriction(pointerX, pointerY);
                init();
                break;
            }
            if (n.isActive()) {
                n.setLastPosition(n.xd(), n.yd(), n.zd());
                n.setByRestriction(pointerX, pointerY);
                init();
                break;
            }
        }
    }

    /**
     * add a tree node by restriction (should be within the boundary polygon)
     *
     * @param pointerX x
     * @param pointerY y
     * @param boundary boundary polygon
     * @return void
     */
    public void addTreeNode(int pointerX, int pointerY, WB_Polygon boundary) {
        TrafficNodeTree tree = new TrafficNodeTree(pointerX, pointerY, boundary);
        tree.setRegionR(MallConstant.MAIN_TRAFFIC_WIDTH * 0.5 * MallConstant.SCALE);
        if (WB_GeometryOp.contains2D(tree.toWB_Point(), boundary) && WB_GeometryOp.getDistance2D(tree.toWB_Point(), boundary) > tree.getRegionR()) {
            treeNodes.add(tree);
            init();
        }
    }

    public void addTreeNode(TrafficNode treeNode, WB_Polygon boundary) {
        if (WB_GeometryOp.contains2D(treeNode.toWB_Point(), boundary) && WB_GeometryOp.getDistance2D(treeNode.toWB_Point(), boundary) > treeNode.getRegionR()) {
            treeNodes.add(treeNode);
            init();
        }
    }

    /**
     * add a fixed node by restriction (should be on the boundary edges)
     *
     * @param pointerX x
     * @param pointerY y
     * @param boundary boundary polygon
     * @return void
     */
    public void addFixedNode(int pointerX, int pointerY, WB_Polygon boundary) {
        TrafficNodeFixed fixed = new TrafficNodeFixed(pointerX, pointerY, boundary);
        fixed.setRegionR(MallConstant.MAIN_TRAFFIC_WIDTH * 0.5 * MallConstant.SCALE);
        fixed.setByRestriction(pointerX, pointerY);
        fixedNodes.add(fixed);
        init();
    }

    public void addFixedNode(TrafficNode fixedNode) {
        fixedNodes.add(fixedNode);
        init();
    }

    /**
     * remove a tree node
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void removeTreeNode(int pointerX, int pointerY) {
        for (int i = 0; i < treeNodes.size(); i++) {
            if (treeNodes.get(i).isMoused(pointerX, pointerY)) {
                treeNodes.remove(i);
                init();
                break;
            }
        }
    }

    /**
     * remove a fixed node
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void removeFixedNode(int pointerX, int pointerY) {
        for (int i = 0; i < fixedNodes.size(); i++) {
            if (fixedNodes.get(i).isMoused(pointerX, pointerY)) {
                fixedNodes.remove(i);
                init();
                break;
            }
        }
    }

    /**
     * update control radius of node
     *
     * @param pointerX x
     * @param pointerY y
     * @param r        radius delta
     * @return void
     */
    public void changeR(int pointerX, int pointerY, double r) {
        for (TrafficNode fixedNode : fixedNodes) {
            if (fixedNode.isMoused(pointerX, pointerY)) {
                fixedNode.updateRegionR(r);
                init();
                break;
            }
        }
        for (TrafficNode treeNode : treeNodes) {
            if (treeNode.isMoused(pointerX, pointerY)) {
                treeNode.updateRegionR(r);
                init();
                break;
            }
        }
    }

    /**
     * set a atrium for tree node
     *
     * @return void
     */
    public void setAtrium() {
        for (TrafficNode n : treeNodes) {
            if (n.getAtrium() != null) {
                n.setAtrium();
            }
        }
    }

    /**
     * add or remove an atrium at a tree node
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void addOrRemoveAtrium(int pointerX, int pointerY) {
        for (TrafficNode n : treeNodes) {
            if (n.isMoused(pointerX, pointerY)) {
                if (!n.hasAtrium()) {
                    n.setAtrium();
                } else {
                    n.clearAtrium();
                }
                init();
                break;
            }
        }
    }

    /**
     * choose a node to adjust its atrium shape
     *
     * @param pointerX x
     * @param pointerY y
     * @return void
     */
    public void chooseAtrium(int pointerX, int pointerY) {
        if (selectedNode == null) {
            // find which is selected
            for (TrafficNode n : treeNodes) {
                if (n.isMoused(pointerX, pointerY)) {
                    if (n.hasAtrium()) {
                        this.selectedNode = n;
                        selectedNode.setAtriumActive(true);
                    }
                    break;
                }
            }
        } else {
            // if selected node exists
            if (selectedNode.isMoused(pointerX, pointerY)) {
                selectedNode.switchAtriumControl();
            }
        }
    }

    /**
     * clear the selected node
     *
     * @return void
     */
    public void clearSelectAtrium() {
        if (selectedNode != null) {
            selectedNode.setAtriumActive(false);
            selectedNode = null;
        }
    }

    /**
     * update the length (along graph edge)
     *
     * @param delta delta length
     * @return void
     */
    public void updateSelectedAtriumLength(double delta) {
        if (selectedNode != null) {
            selectedNode.updateAtriumLength(delta);
            init();
        }
    }

    /**
     * update the width (perpendicular to linked edge)
     *
     * @param delta delta length
     * @return void
     */
    public void updateSelectedAtriumWidth(double delta) {
        if (selectedNode != null) {
            selectedNode.updateAtriumWidth(delta);
            init();
        }
    }

    public void addOrRemoveAtriumEscalator() {
        if (selectedNode != null) {
            selectedNode.getAtrium().addOrRemoveEscalator();
        }
    }

    /* ------------- draw -------------*/

    /**
     * draw all nodes and edges
     *
     * @param render WB_Render
     * @param app    PApplet
     * @return void
     */
    public void display(WB_Render render, PApplet app) {
        app.pushStyle();
        // draw edges
        app.stroke(24, 169, 222);
        app.strokeWeight(2);
        for (ZEdge e : treeEdges) {
            e.display(app);
        }

        for (ZEdge e : fixedEdges) {
            e.display(app);
        }
        // draw nodes
        app.noStroke();
        app.fill(255, 97, 136);
        for (TrafficNode n : treeNodes) {
            ZRender.drawZPoint2D(app, n, 5);
//            n.displayJoint(app, 5);
            n.displayAtrium(render, app);
        }
        app.fill(128);
        for (TrafficNode n : fixedNodes) {
            ZRender.drawZPoint2D(app, n, 5);
//            n.displayJoint(app, 5);
        }
        app.popStyle();
    }

    /**
     * draw neighbor nodes of one node where mouse at
     *
     * @param pointerX x
     * @param pointerY y
     * @param app      PApplet
     * @return void
     */
    public void displayNeighbor(int pointerX, int pointerY, PApplet app) {
        for (TrafficNode n : treeNodes) {
            if (n.isMoused(pointerX, pointerY)) {
                n.displayNeighbor(app);
            }
        }
    }

    /* ------------- compute the graph as minimum spanning tree (private) ------------- */

    /**
     * initialize adjacency matrix of all nodes
     *
     * @return void
     */
    private void matrixInit() {
        this.matrix = new double[treeNodes.size()][treeNodes.size()];
        for (int i = 0; i < treeNodes.size(); i++) {
            for (int j = i; j < treeNodes.size(); j++) {
                if (i == j) {
                    matrix[i][j] = -1;
                } else {
                    matrix[i][j] = matrix[j][i] = treeNodes.get(i).distance(treeNodes.get(j));
                }
            }
        }
    }

    /**
     * compute minimum spanning tree
     *
     * @return void
     */
    private void getTree() {
        treeEdges = new ArrayList<>();
        // list to count each node
        List<Integer> list = new ArrayList<>();
        list.add(0);

        int begin = 0;
        int end = 0;
        float weight;

        // final tree indices (n-1)
        int[] parent = new int[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            parent[i] = -1;
        }

        while (list.size() < matrix.length) {
            weight = MAXVALUE;
            for (Integer row : list) {
                for (int i = 0; i < matrix.length; i++) {
                    if (!list.contains(i)) {
                        if (i >= row + 1) {
                            if (matrix[row][i] >= 0 && matrix[row][i] < weight) {
                                begin = row;
                                end = i;
                                weight = (float) matrix[row][i];
                            }
                        } else if (i <= row - 1) {
                            if (matrix[i][row] >= 0 && matrix[i][row] < weight) {
                                begin = row;
                                end = i;
                                weight = (float) matrix[i][row];
                            }
                        }
                    }
                }
            }
            list.add(end);
            parent[end] = begin;
        }

        // add to ArrayList
        for (int i = 1; i < parent.length; i++) {
            ZEdge edge = new ZEdge(treeNodes.get(i), treeNodes.get(parent[i]));
            treeEdges.add(edge);
        }
    }

    /**
     * add edges from entries to their nearest node
     *
     * @return void
     */
    private void getFixedLink() {
        fixedEdges = new ArrayList<>();
        for (TrafficNode f : fixedNodes) {
            ZEdge edge = new ZEdge(getNearestNode(f, treeNodes), f);
            fixedEdges.add(edge);
        }
    }

    /**
     * find the nearest node
     *
     * @param curr  one node
     * @param other other nodes
     * @return oldVersion.mallElements.TrafficNode
     */
    private TrafficNode getNearestNode(TrafficNode curr, List<TrafficNode> other) {
        double[] dist = new double[other.size()];
        for (int i = 0; i < other.size(); i++) {
            dist[i] = curr.distance(other.get(i));
        }
        int nearestIndex = ZMath.getMinIndex(dist);
        return other.get(nearestIndex);
    }

    /**
     * set node relations and join point on each bisector
     *
     * @return void
     */
    private void setRelations() {
        // clear current node relationship
        for (TrafficNode n : treeNodes) {
            n.setRelationReady();
        }
        for (TrafficNode n : fixedNodes) {
            n.setRelationReady();
        }

        // set relations
        for (ZEdge e : treeEdges) {
            e.getStart().addNeighbor(e.getEnd());
            e.getEnd().addNeighbor(e.getStart());

            e.getStart().addLinkedEdge(e);
            e.getEnd().addLinkedEdge(e);
        }
        for (ZEdge e : fixedEdges) {
            e.getStart().addNeighbor(e.getEnd());
            e.getEnd().addNeighbor(e.getStart());

            e.getStart().addLinkedEdge(e);
            e.getEnd().addLinkedEdge(e);
        }
    }

    /**
     * 找到graph上某节点开始点沿边移动一定距离后的若干个点
     *
     * @param dist 距离
     * @return java.util.List<geometry.ZPoint>
     */
    public List<ZPoint> pointsOnGraphByDist(final TrafficNode node, final double dist) {
        return null;
    }
}
