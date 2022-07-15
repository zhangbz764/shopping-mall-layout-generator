package mallIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fileManager.ZFileOP;
import main.MallGenerator;
import main.MallInteractor;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * description
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2021/6/9
 * @time 11:46
 */
public class SaveLoad {
    private static final String path_interact = "./src/main/resources/i.json";
    private static final String path_generator = "./src/main/resources/g.json";

    /* ------------- constructor ------------- */

    public SaveLoad() {

    }

    /* ------------- member function ------------- */

    public void saveEdit(int status, MallInteractor mallInteract, MallGenerator mallGenerator) {
        GsonBuilder gb = new GsonBuilder().serializeSpecialFloatingPointValues();
        gb.registerTypeAdapter(CoordinateSequence.class, new InterfaceAdapter());
        Gson gson = gb.create();

        String s_interact = gson.toJson(mallInteract);
        String s_generator = gson.toJson(mallGenerator);

        ZFileOP.writeStringToFile(path_interact, status + s_interact);
        ZFileOP.writeStringToFile(path_generator, s_generator);
    }

    public List loadEdit() {
        List objectList = new ArrayList();
        try {
            Gson gson = new Gson();
            String l_interact_raw = ZFileOP.readStringFromFile(path_interact);
            String l_generator = ZFileOP.readStringFromFile(path_generator);
            if (l_interact_raw != null) {
                int status = Integer.parseInt(l_interact_raw.substring(0, 1));
                objectList.add(status);
                String l_interact = l_interact_raw.substring(1);
                objectList.add(gson.fromJson(l_interact, MallInteractor.class));
            }
            System.out.println("fine");
            if (l_generator != null) {
                objectList.add(gson.fromJson(l_generator, MallGenerator.class));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return objectList;
    }
}
