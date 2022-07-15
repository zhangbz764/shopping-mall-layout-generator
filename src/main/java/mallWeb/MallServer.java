package mallWeb;

import archijson.ArchiJSON;
import archijson.ArchiServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.socket.client.Socket;
import mallParameters.MallConst;

/**
 * description
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project archijson
 * @date 2021/3/10
 * @time 17:49
 */
public class MallServer implements ArchiServer {
    private MallJsonProcessor processor;

    /* ------------- constructor ------------- */

    public MallServer() {
        String URL = "https://web.archialgo.com";
        String TOKEN = "bc5726c6-96ff-40e5-8459-353453a05caf";
        String IDENTITY = "mall-java-backend";

        ArchiServer.super.setup(URL, TOKEN, IDENTITY);

        this.processor = new MallJsonProcessor();

        System.out.println(">>> server initialized");
    }

    @Override
    public void onConnect(Socket socket) {
        System.out.println("connected");
    }

    @Override
    public void onReceive(Socket socket, String id, JsonObject body) {
        ArchiServer.super.onReceive(socket, id, body);

        Gson gson = new Gson();

        // archijson received
        ArchiJSON jsonR = gson.fromJson(body, ArchiJSON.class);
        jsonR.parseGeometryElements(gson);
        int statusID = jsonR.getProperties().get("statusID").getAsInt();
        int functionID = jsonR.getProperties().get("functionID").getAsInt();
        System.out.println("statusID: " + statusID + "  " + "functionID: " + functionID);

        // archijson to send
        ArchiJSON jsonS = new ArchiJSON();
        JsonObject properties = new JsonObject();
        properties.addProperty("statusID", statusID);
        properties.addProperty("functionID", functionID);

        switch (statusID) {
            case MallConst.E_SITE_BOUNDARY:
                processor.processStatus0(functionID, jsonR, jsonS, properties);
                break;
            case MallConst.E_TRAFFIC_ATRIUM:
                processor.processStatus1(functionID, jsonR, jsonS, properties);
                break;
            case MallConst.E_STRUCTURE_GRID:
                processor.processStatus4(functionID, jsonR, jsonS, properties);
                break;
        }

        ArchiServer.super.send(socket, "client", id, gson.toJson(jsonS));
    }

//    @Deprecated
//    public MallServer(String... args) {
//        try {
//            if (args.length > 0) {
//                socket = IO.socket(args[0]);
//                this.setupServer();
//                System.out.println("Socket connected to " + args[0]);
//            } else {
//                String url = "http://8.136.121.130:" + PORT;  // aliyun
////                String url = "http://10.192.2.153:" + PORT;  // local
//                socket = IO.socket(url);
//                this.setupServer();
//                System.out.println("Socket connected to " + url);
//            }
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /* ------------- member function ------------- */
//
//    /**
//     * receiving, converting, processing and returning data
//     *
//     * @return void
//     */
//    public void setupServer() {
//        Gson gson = new Gson();
//
//        generator = new MallGenerator();
//        socket.connect();
//
//        /*
//        setBoundary_receive, clear all other properties
//        初始化 generator, 初始化全部floor (new)
//         */
//        socket.on("ftb:receiveInit", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // initializing
//            WB_Polygon p = WB_Converter.toWB_Polygon((Segments) archijson.getGeometries().get(0));
//            generator.setBoundary_receive(ZTransform.validateWB_Polygon(p));
//            generator.setInnerNode_receive(null);
//            generator.setEntryNode_receive(null);
//            generator.setRawAtrium_receive(null);
//            generator.setBufferCurve_receive(null);
//            generator.setCellPolys_receive(null);
//
////            generator.init();
//        });
//
//        /*
//        setInnerNode_receive, setEntryNode_receive, setPolyAtrium_receive
//        生成graph和动线边界
//        -> floorNum (essential)
//        -> atriumNum
//        -> bufferDist
//        -> curvePoints
//         */
//        socket.on("ftb:receiveNodesAndAtriums", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // converting
//            int floorNum = archijson.getProperties().getFloorNum(); // 当前层数
//            int atriumNum = archijson.getProperties().getAtriumNum(); // 中庭图元个数
//            double bufferDist = archijson.getProperties().getBufferDist(); // 偏移距离
//            int curvePoints = archijson.getProperties().getCurvePoints(); // 曲线重构点数
//
//            status1Setting(archijson, atriumNum);
//
//            // processing
//            generator.generateGraphAndBuffer(floorNum, bufferDist, curvePoints);
//
//            // return
//            ArchiJSON json = generator.toArchiJSONGraphAndBuffer(floorNum, archijson.getId(), gson);
//            socket.emit("btf:sendGraphAndBuffer", gson.toJson(json));
//        });
//
//        /*
//        setBufferCurve_receive
//        划分公共与商铺区块，剖分小店铺
//        -> floorNum (essential)
//        -> span
//         */
//        socket.on("ftb:receiveBuffer", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // converting
//            int floorNum = archijson.getProperties().getFloorNum(); // 当前层数
//            double span = archijson.getProperties().getSpan(); // 剖分跨度
//
//            status2Setting(floorNum, archijson);
//
//            // processing
//            generator.generateSubdivision(floorNum);
//
//            // return
//            ArchiJSON json = generator.toArchiJSONSubdivision(floorNum, archijson.getId(), gson);
//            socket.emit("btf:sendSubdivision", gson.toJson(json));
//        });
//
//        socket.on("ftb:receiveUnion", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // converting
//            int floorNum = archijson.getProperties().getFloorNum(); // 当前层数
//            double span = archijson.getProperties().getSpan(); // 剖分跨度
//
//            status2Setting(floorNum, archijson);
//
//            // processing
//            generator.generateSubdivision(floorNum);
//
//            // return
//            ArchiJSON json = generator.toArchiJSONSubdivision(floorNum, archijson.getId(), gson);
//            socket.emit("btf:sendUnion", gson.toJson(json));
//        });
//
//        /*
//        setCellPolys_receive
//        生成疏散楼梯点位
//        -> floorNum (essential)
//         */
//        socket.on("ftb:receiveSubdivision", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // converting
//            int floorNum = archijson.getProperties().getFloorNum(); // 当前层数
//
//            status3Setting(floorNum, archijson);
//
//            // processing
//            generator.generateEvacuation(floorNum);
//
//            // return
//            ArchiJSON json = generator.toArchiJSONEvacuation(floorNum, archijson.getId(), gson);
//            socket.emit("btf:sendEvacuation", gson.toJson(json));
//        });
//
//        /*
//
//         */
//        socket.on("ftb:receiveFloorChange", args -> {
//            // receiving
//            ArchiJSON archijson = gson.fromJson(args[0].toString(), ArchiJSON.class);
//            archijson.parseGeometryElements(gson);
//
//            // converting and processing
//            int floorNum = archijson.getProperties().getFloorNum(); // 当前层数
//
//            // if current floor status = 0, make status 1 and send
//            // if current floor status = 1, 2 or 3, send normally
//            int atriumNum = archijson.getProperties().getAtriumNum(); // 中庭图元个数
//            double bufferDist = archijson.getProperties().getBufferDist(); // 偏移距离
//            int curvePoints = archijson.getProperties().getCurvePoints(); // 曲线重构点数
//            status1Setting(archijson, atriumNum);
//            generator.generateGraphAndBuffer(floorNum, bufferDist, curvePoints);
//
//            // return
//            ArchiJSON json = generator.toArchiJSONFloor(floorNum, archijson.getId(), gson);
//            socket.emit("btf:sendFloorChange", gson.toJson(json));
//        });
//    }
//
//    /**
//     * generator setting from frontend
//     * status 1
//     *
//     * @param archijson ArchiJSON received
//     * @param atriumNum number of atriums
//     * @return void
//     */
//    private void status1Setting(ArchiJSON archijson, int atriumNum) {
//        generator.setInnerNode_receive(
//                WB_Converter.toWB_Point((Vertices) archijson.getGeometries().get(0))
//        );
//        generator.setEntryNode_receive(
//                WB_Converter.toWB_Point((Vertices) archijson.getGeometries().get(1))
//        );
//        List<WB_Polygon> polyAtrium_receive = new ArrayList<>();
//        for (int i = 2; i < atriumNum + 2; i++) {
//            BaseGeometry g = archijson.getGeometries().get(i);
//            if (g instanceof Segments) {
//                WB_Polygon p = WB_Converter.toWB_Polygon((Segments) g);
////                for (int j = 0; j < p.getNumberOfPoints(); j++) {
////                    System.out.println(p.getPoint(j).toString());
////                }
//                polyAtrium_receive.add(p);
//            } else if (g instanceof Plane) {
//                polyAtrium_receive.add(WB_Converter.toWB_Polygon((Plane) g));
//            }
//        }
//        generator.setRawAtrium_receive(polyAtrium_receive);
//    }
//
//    /**
//     * generator setting from frontend
//     * status 2
//     *
//     * @param archijson ArchiJSON received
//     * @return void
//     */
//    private void status2Setting(int floorNum, ArchiJSON archijson) {
//        List<LineString> bufferCurve = new ArrayList<>();
//        for (int i = 0; i < archijson.getGeometries().size(); i++) {
//            BaseGeometry g = archijson.getGeometries().get(i);
//            if (g instanceof Segments) {
//                WB_PolyLine pl = WB_Converter.toWB_Polyline((Segments) g);
//                bufferCurve.add(ZTransform.WB_PolyLineToLineString(pl));
//            }
//        }
//        generator.setBufferCurve_receive(floorNum, bufferCurve);
//    }
//
//    /**
//     * generator setting from frontend
//     * status 3
//     *
//     * @param archijson ArchiJSON received
//     * @return void
//     */
//    private void status3Setting(int floorNum, ArchiJSON archijson) {
//        List<WB_Polygon> cellPolys_receive = new ArrayList<>();
//        for (int i = 0; i < archijson.getGeometries().size(); i++) {
//            BaseGeometry g = archijson.getGeometries().get(i);
//            if (g instanceof Segments) {
//                WB_Polygon poly = WB_Converter.toWB_Polygon((Segments) g);
//                cellPolys_receive.add(poly);
//            }
//        }
//        generator.setCellPolys_receive(floorNum, cellPolys_receive);
//    }
}