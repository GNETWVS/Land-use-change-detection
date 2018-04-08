package LandUseChangeDetection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import java.awt.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

class ChangeDetector {

    /**
     * Before Sentinel 2 data
     */
    private SentinelData beforeSentinelData;

    /**
     * After Sentinel 2 data
     */
    private SentinelData afterSentinelData;

    ChangeDetector(SentinelData beforeSentinelData, SentinelData afterSentinelData) throws Exception {
        // TODO: Проверка дат
        this.beforeSentinelData = beforeSentinelData;
        this.afterSentinelData = afterSentinelData;

        cropScenes();
    }

    private void cropScenes() throws Exception {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        beforeSentinelData.cropBands(afterSentinelData.getEnvelope());
        afterSentinelData.cropBands(beforeSentinelData.getEnvelope());
    }

    FeatureCollection getChanges() throws Exception {
        if (beforeSentinelData.getHeight() != afterSentinelData.getHeight()
                || beforeSentinelData.getWidth() != afterSentinelData.getWidth()) {
            return null;
        }
        Classification svm = Classification.getInstance();
        Raster beforeMask = beforeSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        Raster afterMask = afterSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        int[] beforeMaskPixels = new int[beforeMask.getWidth() * beforeMask.getHeight()];
        beforeMask.getPixels(beforeMask.getMinX(), beforeMask.getMinY(), beforeMask.getWidth(), beforeMask.getHeight(), beforeMaskPixels);
        int[] afterMaskPixels = new int[afterMask.getWidth() * afterMask.getHeight()];
        afterMask.getPixels(afterMask.getMinX(), afterMask.getMinY(), afterMask.getWidth(), afterMask.getHeight(), afterMaskPixels);
        float[][] beforeClassification = new float[beforeSentinelData.getWidth()][beforeSentinelData.getHeight()];
        float[][] afterClassification = new float[afterSentinelData.getWidth()][afterSentinelData.getHeight()];
        beforeSentinelData.getPixelVector(0);
        afterSentinelData.getPixelVector(0);
        int width = beforeSentinelData.getWidth();
        int height = beforeSentinelData.getHeight();
        IntStream.range(0, width - 1).parallel().forEach(x ->
            IntStream.range(0, height - 1).parallel().forEach(y -> {
                System.out.println(x + " " + y);
                int i = x * height + y;
                if (beforeMaskPixels[i] != 1 && afterMaskPixels[i] != 1) {
                    beforeClassification[x][y] = svm.predict(beforeSentinelData.getPixelVector(i));
                    afterClassification[x][y] = svm.predict(afterSentinelData.getPixelVector(i));
                } else {
                    beforeClassification[x][y] = -1;
                    afterClassification[x][y] = -1;
                }
            })
        );
        // Check pixels
        checkPixels(beforeClassification);
        checkPixels(afterClassification);
        // Create classification raster
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D beforeClassesGrid = factory.create("Before classes", beforeClassification, beforeSentinelData.getEnvelope());
        GridCoverage2D afterClassesGrid = factory.create("After classes", afterClassification, afterSentinelData.getEnvelope());
        // Raster to vector
        final PolygonExtractionProcess process = new PolygonExtractionProcess();
        System.out.println("Polygon Extraction Before");
        SimpleFeatureCollection beforeCollection = process.execute(beforeClassesGrid,  0, true,
                null, Collections.singletonList(-1), null, null);
        //beforeCollection = Utils.transformToCRSWithAttributes(beforeCollection, DefaultGeographicCRS.WGS84);
        System.out.println("Polygon Extraction After");
        System.out.println(beforeCollection.getSchema().getCoordinateReferenceSystem());
        SimpleFeatureCollection afterCollection = process.execute(afterClassesGrid, 0, true,
                null, Collections.singletonList(-1), null, null);
        //afterCollection = Utils.transformToCRSWithAttributes(afterCollection, DefaultGeographicCRS.WGS84);
        System.out.println("Finish polygon extraction");
        // Get land use changes
        SimpleFeatureCollection collection = getIntersections(beforeCollection, afterCollection);
        writeShapefile(beforeCollection, "1.shp");
        writeShapefile(afterCollection, "2.shp");
        writeShapefile(collection, "3.shp");
        return collection;
    }

    private void writeShapefile(SimpleFeatureCollection collection, String name) throws IOException {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        File file = new File("C:\\Users\\Arthur\\Desktop\\1\\" + name);
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createDataStore(params);
        dataStore.createSchema(collection.getSchema());
        Transaction transaction = new DefaultTransaction("create");
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }

    /**
     * Checking for pixel neighbors
     * @param pixels pixels matrix
     */
    private void checkPixels(float[][] pixels) {
        IntStream.range(0, pixels.length - 1).parallel().forEach(x ->
            IntStream.range(0, pixels[x].length - 1).parallel().forEach(y -> {
                System.out.println("Ch " + x + " " + y);
                float val = pixels[x][y];
                if (val != -1) {
                    List<Float> neighbors = new ArrayList<>();
                    for (int i = x - 1; i <= x + 1; ++i) {
                        for (int j = y - 1; j <= y + 1; ++j) {
                            if ((x == i && y == j)
                                    || i < 0
                                    || j < 0
                                    || i >= pixels.length
                                    || j >= pixels[0].length
                                    || pixels[i][j] == -1) {
                                continue;
                            }
                            neighbors.add(pixels[i][j]);
                        }
                    }
                    if (neighbors.size() > 0) {
                        float c = neighbors.get(0);
                        boolean flag = true;
                        for (float v : neighbors) {
                            if (v != c) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            pixels[x][y] = c;
                        }
                    }
                }
            })
        );
}



//    /**
//     * Get cropped and merged clouds and snow mask
//     * @return cropped and merged Sentinel 2 data mask
//     * @throws Exception if cannot read masks files
//     */
//    private GridCoverage2D getCloudsAndSnowMask() throws Exception {
//        // Check sizes
//        GridCoverage2D beforeMask = beforeSentinelData.getCloudsAndSnowMask();
//        GridCoverage2D afterMask = afterSentinelData.getCloudsAndSnowMask();
//        // JAI merging
//        ParameterBlock mergeOp = new ParameterBlock();
//        mergeOp.addSource(beforeMask.getRenderedImage());
//        mergeOp.addSource(afterMask.getRenderedImage());
//        //JAIExt.initJAIEXT();
//        RenderedOp mask = JAI.create("Or", mergeOp);
//
//        GridCoverageFactory factory = new GridCoverageFactory();
//        ReferencedEnvelope envelope = new ReferencedEnvelope(beforeMask.getEnvelope());
//        ReferencedEnvelope envelope1 = new ReferencedEnvelope(afterMask.getEnvelope());
//        return factory.create("Clouds and snow mask", mask, envelope);
//    }

    /**
     * Filter factory
     */
    private static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    /**
     * Get land-use collections changes
     * @param beforeCollection before sensed land-use collection
     * @param afterCollection after sensed land-use collection
     * @return land-use change collection
     */
    private static SimpleFeatureCollection getIntersections(SimpleFeatureCollection beforeCollection, SimpleFeatureCollection afterCollection) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("LUCD");
        typeBuilder.setCRS(beforeCollection.getSchema().getCoordinateReferenceSystem());
        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("before", Integer.class);
        typeBuilder.add("after", Integer.class);
        final SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        DefaultFeatureCollection collection = new DefaultFeatureCollection(null, null);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        String geomPropertyName = afterCollection.getSchema().getGeometryDescriptor().getLocalName();
        SimpleFeatureIterator it = beforeCollection.features();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Filter filter = ff.intersects(ff.property(geomPropertyName), ff.literal(geometry));
            SimpleFeatureCollection interCollection = afterCollection.subCollection(filter);
            if (interCollection != null) {
                SimpleFeatureIterator sfi = interCollection.features();
                while (sfi.hasNext()) {
                    SimpleFeature afterFeature = sfi.next();
                    Geometry afterGeometry = (Geometry) afterFeature.getDefaultGeometry();
                    if (afterGeometry instanceof Polygon || afterCollection instanceof MultiPolygon) {
                        Geometry intersection = geometry.intersection(afterGeometry);
                        if (!intersection.isEmpty()) { // TODO: Types
                            intersection.setUserData(geometry);
                            featureBuilder.add(intersection);
                            featureBuilder.add(feature.getAttribute("value"));
                            featureBuilder.add(afterFeature.getAttribute("value"));
                            SimpleFeature intersectionFeature = featureBuilder.buildFeature(null);
                            collection.add(intersectionFeature);
                        }
                    }
                }
            }
        }
        return collection;
    }
}
