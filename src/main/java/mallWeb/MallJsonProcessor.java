package mallWeb;

import archijson.ArchiJSON;
import basicGeometry.ZLine;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import converter.JTS_Converter;
import converter.WB_Converter;
import geometry.Plane;
import geometry.Segments;
import geometry.Vertices;
import main.MallGenerator;
import mallElementNew.StructureGrid;
import mallIO.ImportData;
import mallParameters.MallConst;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import wblut.geom.WB_Coord;
import wblut.geom.WB_Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description
 *
 * @author ZHANG Baizhou zhangbz
 * @project shopping_mall
 * @date 2022/2/18
 * @time 13:21
 */
public class MallJsonProcessor {
    private final ImportData input = new ImportData(); // temp
    private final MallGenerator mg;
    private final Gson gson = new Gson();

    /* ------------- constructor ------------- */

    public MallJsonProcessor() {
        // local import
        String importPath = "./src/main/resources/0310.3dm";
        this.input.loadData(importPath);

        this.mg = new MallGenerator();
    }

    /* ------------- member function ------------- */

    /**
     * back-end process for edit status 0
     *
     * @param functionID ID of function
     * @param jsonR      received Archijson
     * @param jsonS      Archijson to send
     * @param properties properties to send
     * @return void
     */
    public void processStatus0(
            int functionID,
            ArchiJSON jsonR,
            ArchiJSON jsonS,
            JsonObject properties
    ) {
        List<JsonElement> elements = new ArrayList<>();

        Segments boundary, site;

        switch (functionID) {
            case MallConst.INIT_FLAG:
                mg.initSiteBoundary(
                        input.getInputBlock(),
                        input.getInputBoundary(),
                        0,
                        MallConst.SITE_REDLINEDIST_DEFAULT,
                        MallConst.SITE_BUFFER_DEFAULT
                );

                boundary = JTSConverter.toSegments(mg.getSiteBase().getBoundary());
                site = JTSConverter.toSegments(mg.getSiteBase().getSite());
                elements.add(gson.toJsonTree(boundary));
                elements.add(gson.toJsonTree(site));
                break;
            case MallConst.BUTTON_SWITCH_BOUNDARY:
            case MallConst.SLIDER_OFFSET_DIST:
            case MallConst.SLIDER_REDLINE_DIST:
                int boundaryBase = jsonR.getProperties().get("boundaryBase").getAsInt();
                double redLineDist = jsonR.getProperties().get("redLineDist").getAsDouble();
                double siteOffsetDist = jsonR.getProperties().get("siteOffsetDist").getAsDouble();
                mg.updateSiteBaseL(boundaryBase, redLineDist, siteOffsetDist);

                boundary = JTSConverter.toSegments(mg.getSiteBase().getBoundary());
                elements.add(gson.toJsonTree(boundary));
                break;
        }

        jsonS.setGeometryElements(elements);
        jsonS.setProperties(properties);
    }

    /**
     * back-end process for edit status 1
     *
     * @param functionID ID of function
     * @param jsonR      received Archijson
     * @param jsonS      Archijson to send
     * @param properties properties to send
     * @return void
     */
    public void processStatus1(
            int functionID,
            ArchiJSON jsonR,
            ArchiJSON jsonS,
            JsonObject properties
    ) {
        List<JsonElement> elements = new ArrayList<>();

        Segments trafficPath, trafficBuffer;
        Vertices entryVer, innerVer;
        List<WB_Coord> entryCoords,innerCoords;
        double bufferDist;

        switch (functionID) {
            case MallConst.INIT_FLAG:
                Polygon boundary_receive = JTSConverter.toPolygon((Segments) jsonR.getGeometries().get(0));

                mg.updateBoundary(boundary_receive);
                mg.initTraffic(MallConst.TRAFFIC_BUFFER_DIST_DEFAULT);

                trafficPath = JTSConverter.toSegments(mg.getMainTraffic().getMainTrafficCurve());
                trafficBuffer = JTSConverter.toSegments(mg.getMainTraffic().getMainTrafficBuffer());
                entryCoords = new ArrayList<>(mg.getMainTraffic().getEntryNodes());
                entryVer = WB_Converter.toVertices(entryCoords, 3);
                innerCoords = new ArrayList<>(mg.getMainTraffic().getInnerNodes());
                innerVer = WB_Converter.toVertices(innerCoords, 3);

                elements.add(gson.toJsonTree(trafficPath));
                elements.add(gson.toJsonTree(trafficBuffer));
                elements.add(gson.toJsonTree(entryVer));
                elements.add(gson.toJsonTree(innerVer));
                break;
            case MallConst.DRAG_TRAFFIC_CTRL:
                List<WB_Point> entryNode_receive = WB_Converter.toWB_Point((Vertices) jsonR.getGeometries().get(0));
                List<WB_Point> innerNode_receive = WB_Converter.toWB_Point((Vertices) jsonR.getGeometries().get(1));
                bufferDist = jsonR.getProperties().get("trafficWidth").getAsDouble();

                mg.updateTrafficByRawNodes(entryNode_receive, innerNode_receive, bufferDist);

                trafficPath = JTSConverter.toSegments(mg.getMainTraffic().getMainTrafficCurve());
                trafficBuffer = JTSConverter.toSegments(mg.getMainTraffic().getMainTrafficBuffer());
                entryCoords = new ArrayList<>(mg.getMainTraffic().getEntryNodes());
                entryVer = WB_Converter.toVertices(entryCoords, 3);
                innerCoords = new ArrayList<>(mg.getMainTraffic().getInnerNodes());
                innerVer = WB_Converter.toVertices(innerCoords, 3);

                elements.add(gson.toJsonTree(trafficPath));
                elements.add(gson.toJsonTree(trafficBuffer));
                elements.add(gson.toJsonTree(entryVer));
                elements.add(gson.toJsonTree(innerVer));
                break;
            case MallConst.SLIDER_TRAFFIC_WIDTH:
                bufferDist = jsonR.getProperties().get("trafficWidth").getAsDouble();

                mg.updateTrafficWidth(bufferDist);

                trafficBuffer = JTSConverter.toSegments(mg.getMainTraffic().getMainTrafficBuffer());

                elements.add(gson.toJsonTree(trafficBuffer));
                break;
            case MallConst.DBCLICK_ADD_ATRIUM:

                break;
        }

        jsonS.setGeometryElements(elements);
        jsonS.setProperties(properties);
    }

    /**
     * back-end process for edit status 0
     *
     * @param functionID ID of function
     * @param jsonR      received Archijson
     * @param jsonS      Archijson to send
     * @param properties properties to send
     * @return void
     */
    public void processStatus4(
            int functionID,
            ArchiJSON jsonR,
            ArchiJSON jsonS,
            JsonObject properties
    ) {
        List<JsonElement> elements = new ArrayList<>();

        switch (functionID) {
            case MallConst.INIT_FLAG:
                mg.initGrid(MallConst.STRUCTURE_GRID_NUM, MallConst.STRUCTURE_MODEL);

                break;
            case MallConst.BUTTON_GRID_8:
            case MallConst.BUTTON_GRID_9:
                double gridModulus = jsonR.getProperties().get("gridModulus").getAsDouble();
                mg.updateGridModulus(gridModulus);

                break;
            case MallConst.BUTTON_GRIDNUM_1:
            case MallConst.BUTTON_GRIDNUM_2:
            case MallConst.BUTTON_GRIDNUM_3:
                int gridNum = jsonR.getProperties().get("gridNum").getAsInt();
                mg.initGrid(gridNum, MallConst.STRUCTURE_MODEL);

                break;
        }

        List<Segments> gridLines = new ArrayList<>();
        for (int i = 0; i < mg.getGrids().length; i++) {
            StructureGrid g = mg.getGrids()[i];
            for (ZLine l : g.getAllLines()) {
                LineString ls = l.toJtsLineString();
                Segments seg = JTSConverter.toSegments(ls);
                elements.add(gson.toJsonTree(seg));
            }
        }

        jsonS.setGeometryElements(elements);
        jsonS.setProperties(properties);
    }

    /* ------------- setter & getter ------------- */


}
