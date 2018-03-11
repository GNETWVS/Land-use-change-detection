package LandUseChangeDetection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

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

    public static SimpleFeatureCollection transformToCRS(SimpleFeatureCollection fc, CoordinateReferenceSystem crs) throws FactoryException {
        CoordinateReferenceSystem vectorCRS = fc.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(vectorCRS, crs)) {
            MathTransform transform = CRS.findMathTransform(vectorCRS, crs, true);
            // Create transformed feature collection
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName(fc.getSchema().getName());
            typeBuilder.setCRS(crs);
            typeBuilder.add("geom", MultiPolygon.class);
            final SimpleFeatureType featureType = typeBuilder.buildFeatureType();
            DefaultFeatureCollection transformedFC = new DefaultFeatureCollection(null, null);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

            try (SimpleFeatureIterator it = fc.features()) {
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    geometry = JTS.transform(geometry, transform);
                    featureBuilder.add(geometry);
                    SimpleFeature transformedFeature = featureBuilder.buildFeature(null);
                    transformedFC.add(transformedFeature);
                }
            } catch (TransformException e) {
                e.printStackTrace();
            }
            // Replace collection
            fc = transformedFC;
        }
        return fc;
    }

    // TODO: Safe attributes
}
