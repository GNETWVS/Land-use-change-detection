package LandUseChangeDetection;

import LandUseChangeDetection.data.Data;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.raster.PolygonExtractionProcess;

import java.awt.image.Raster;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;

class ChangeDetector {

    private SentinelData beforeSentinelData;

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
//         clouds and snow masks
//        GridCoverage2D maskGrid = SentinelData.mergeMasks(
//                beforeSentinelData.getCloudsAndSnowMask(),
//                afterSentinelData.getCloudsAndSnowMask());
//        Raster mask = maskGrid.getRenderedImage().getData();
//        if (mask.getWidth() != beforeSentinelData.getWidth() || mask.getHeight() != beforeSentinelData.getHeight()) {
//            throw new Exception("Error, mask and bands sizes should be equal");
//        }
//        int[] maskPixels = new int[mask.getWidth() * mask.getHeight()];
//        mask.getPixels(mask.getMinX(), mask.getMinY(), mask.getWidth(), mask.getHeight(), maskPixels);
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
//        for (int x = 0; x < beforeSentinelData.getWidth(); ++x) {
//            for (int y = 0; y < beforeSentinelData.getHeight(); ++y) {
//                int i = x * beforeSentinelData.getHeight() + y;
//                System.out.println(x + " " + y);
//                if (beforeMaskPixels[i] != 1 && afterMaskPixels[i] != 1) {
//                    beforeClassification[x][y] = svm.predict(beforeSentinelData.getPixelVector(i));
//                    afterClassification[x][y] = svm.predict(afterSentinelData.getPixelVector(i));
//                } else {
//                    beforeClassification[x][y] = -1;
//                    afterClassification[x][y] = -1;
//                }
//            }
//        }
        // Check pixels
        checkPixels(beforeClassification);
        System.out.println("AAAAAAAAAAAAAAAAA");
        checkPixels(afterClassification);
        // Create classification raster
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D beforeClassesGrid = factory.create("Before classes", beforeClassification, beforeSentinelData.getEnvelope());
        GridCoverage2D afterClassesGrid = factory.create("After classes", afterClassification, afterSentinelData.getEnvelope());
        // Raster to vector
        final PolygonExtractionProcess process = new PolygonExtractionProcess();
        System.out.println("before");
        SimpleFeatureCollection beforeCollection = process.execute(beforeClassesGrid,  0, true,
                null, Collections.singletonList(-1), null, null);
        System.out.println("after");
        SimpleFeatureCollection afterCollection = process.execute(afterClassesGrid, 0, true,
                null, Collections.singletonList(-1), null, null);
        System.out.println("afterf");

//        file = new File("C:\\Users\\Arthur\\Desktop\\1\\2.shp");
//        dataStoreFactory = new ShapefileDataStoreFactory();
//        params = new HashMap<>();
//        params.put("url", file.toURI().toURL());
//        params.put("create spatial index", Boolean.TRUE);
//        dataStore = (ShapefileDataStore) dataStoreFactory.createDataStore(params);
//        dataStore.createSchema(afterCollection.getSchema());
//        transaction = new DefaultTransaction("create");
//        typeName = dataStore.getTypeNames()[0];
//        featureSource = dataStore.getFeatureSource(typeName);
//        if (featureSource instanceof SimpleFeatureStore) {
//            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
//            featureStore.setTransaction(transaction);
//            try {
//                featureStore.addFeatures(afterCollection);
//                transaction.commit();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                transaction.rollback();
//            } finally {
//                transaction.close();
//            }
//        }
        // Get land use changes
        SimpleFeatureCollection collection = Data.getLandUseChanges(beforeCollection, beforeSentinelData.getSensingDate(),
                afterCollection, afterSentinelData.getSensingDate());
                File file = new File("C:\\Users\\Arthur\\Desktop\\1\\1.shp");
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
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
        return collection;
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
//        for (int x = 0; x < pixels.length; ++x) {
//            for (int y = 0; y < pixels.length; ++y) {
//                float val = pixels[x][y];
//                if (val == -1) {
//                    continue;
//                }
//                List<Float> neighbors = new ArrayList<>();
//                for (int i = x - 1; i <= x + 1; ++i) {
//                    for (int j = y - 1; j <= y + 1; ++j) {
//                        if ((x == i && y == j)
//                                || i < 0
//                                || j < 0
//                                || i >= pixels.length
//                                || j >= pixels[0].length
//                                || pixels[i][j] == -1) {
//                            continue;
//                        }
//                        neighbors.add(pixels[i][j]);
//                    }
//                }
//                if (neighbors.size() > 0) {
//                    float c = neighbors.get(0);
//                    boolean flag = true;
//                    for (float v : neighbors) {
//                        if (v != c) {
//                            flag = false;
//                            break;
//                        }
//                    }
//                    if (flag) {
//                        pixels[x][y] = c;
//                    }
//                }
//            }
//        }
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
}
