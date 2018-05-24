package landUseChangeDetection;

import java.io.Serializable;
import java.util.List;

public class CDSer implements Serializable {

    List<LandUseChangeDetectionResult> resultList;

    String json;

    String wkt;

    CDSer() {

    }

    public CDSer(List<LandUseChangeDetectionResult> list, String json, String wkt) {
        this.resultList = list;
        this.json = json;
        this.wkt = wkt;
    }
}
