package LandUseChangeDetection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import javafx.scene.control.Alert;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
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

    /**
     * Check for level
     * @param file sentinel 2 data
     * @return existing
     */
    public static boolean isLevel2A(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        String[] files = file.list();
        if (files == null) {
            return false;
        }
        for (String s : files) {
            if (s.equals("MTD_MSIL2A.xml")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Show JavaFX error message
     * @param title message window title
     * @param header message header
     * @param content message content
     */
    public static void showErrorMessage(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Crop grid coverage by envelope
     * @param scene grid coverage
     * @param envelope envelope
     * @return cropped grid coverage
     */
    public static GridCoverage2D cropGridCoverage(GridCoverage2D scene, Envelope envelope) {
        final CoverageProcessor processor = new CoverageProcessor();
        ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
        params.parameter("Envelope").setValue(envelope);
        params.parameter("Source").setValue(scene);
        return (GridCoverage2D)processor.doOperation(params);
    }
}