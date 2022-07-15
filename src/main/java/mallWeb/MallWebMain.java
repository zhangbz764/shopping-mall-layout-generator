package mallWeb;

import java.util.Date;

/**
 * description
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project archijson
 * @date 2021/3/10
 * @time 17:46
 */
public class MallWebMain {
    public static void main(String[] args) {
        System.out.println(">>> back-end server start at: " + new Date(System.currentTimeMillis()));
        new MallServer();
//        if (args.length > 0) {
//            new MallServer(args);
//        } else {
//            PApplet.main("webMain.MallShow");
//        }
    }
}
