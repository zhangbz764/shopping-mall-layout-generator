package main.demoTests;

import processing.core.PApplet;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render;

/**
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/29
 * @time 15:34
 * @description
 */
public class TestDistCloset extends PApplet {
    public void settings() {
        size(1000, 1000, P2D);
    }

    WB_Point origin;
    WB_Polygon poly;
    WB_Point closet;

    WB_Render render;

    public void setup() {
        render = new WB_Render(this);
        origin = new WB_Point(300, 200);

        WB_Point[] pts = new WB_Point[6];
        pts[0] = new WB_Point(100, 100);
        pts[1] = new WB_Point(700, 100);
        pts[2] = new WB_Point(800, 400);
        pts[3] = new WB_Point(500, 800);
        pts[4] = new WB_Point(100, 600);
        pts[5] = new WB_Point(100, 100);
        poly = new WB_Polygon(pts);

        closet = WB_GeometryOp.getClosestPoint2D(origin, poly);
        println(WB_GeometryOp.contains2D(origin, poly));
    }

    public void draw() {
        background(255);
        noFill();
        render.drawPolyLine2D(poly);
        render.drawPoint2D(origin, 20);
        render.drawPoint2D(closet, 20);
    }

}