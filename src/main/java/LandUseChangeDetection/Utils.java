package LandUseChangeDetection;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.TreeMap;

public class Utils {

    /**
     * Read shapefile layers
     * @param shpFile ERSI shapefile
     * @return shapefile data store
     * @throws IOException if file is not exists
     */
    public static DataStore openShapefile(File shpFile) throws IOException {
        Map<String, Object> map = new TreeMap<>();
        try {
            map.put("url", shpFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return DataStoreFinder.getDataStore(map);
    }
}
