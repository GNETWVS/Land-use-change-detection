package LandUseChangeDetection;

import LandUseChangeDetection.data.Data;
import com.vividsolutions.jts.geom.*;
import javafx.concurrent.Task;
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
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
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

    private SimpleFeatureCollection beforeClassification;

    private SimpleFeatureCollection afterClassification;

    private SimpleFeatureCollection changeDetection;

    SimpleFeatureCollection getChangeDetection() {
        return  this.changeDetection;
    }

    private List<LandUseChangeDetectionResult> areas;

    List<LandUseChangeDetectionResult> getAreas() {
        return this.areas;
    }

    ChangeDetector(SentinelData beforeSentinelData, SentinelData afterSentinelData) throws Exception {
        if (beforeSentinelData.getSensingDate().before(afterSentinelData.getSensingDate())) {
            this.beforeSentinelData = beforeSentinelData;
            this.afterSentinelData = afterSentinelData;
        } else {
            this.beforeSentinelData = afterSentinelData;
            this.afterSentinelData = beforeSentinelData;
        }

        cropScenes();
    }

    ChangeDetector(SentinelData beforeSentinelData, SentinelData afterSentinelData, SimpleFeatureCollection collection) throws Exception {
        if (beforeSentinelData.getSensingDate().before(afterSentinelData.getSensingDate())) {
            this.beforeSentinelData = beforeSentinelData;
            this.afterSentinelData = afterSentinelData;
        } else {
            this.beforeSentinelData = afterSentinelData;
            this.afterSentinelData = beforeSentinelData;
        }

        cropScenes();
        cropByROI(collection);
    }

    private void cropScenes() throws Exception {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        beforeSentinelData.cropBands(afterSentinelData.getEnvelope());
        afterSentinelData.cropBands(beforeSentinelData.getEnvelope());
    }

    private void cropByROI(SimpleFeatureCollection collection) throws Exception {
        if (beforeSentinelData == null || afterSentinelData == null || collection == null) {
            return;
        }
        if (beforeSentinelData.getCRS() != collection.getSchema().getCoordinateReferenceSystem()) {
            collection = Utils.transformToCRS(collection, beforeSentinelData.getCRS());
        }
        // Get geom union
        Geometry union = null;
        SimpleFeatureIterator it = collection.features();
        while (it.hasNext()) {
            Geometry geometry = (Geometry) it.next().getDefaultGeometry();
            if (geometry == null) {
                continue;
            }
            if (union == null) {
                union = geometry;
            } else {
                union = union.union(geometry);
            }
        }

        beforeSentinelData.cropBands(union);
        beforeSentinelData.cropBands(union);
    }

    private float[][] beforeClassificationMatrix;
    private float[][] afterClassificationMatrix;

    void certificate() throws Exception {
        if (beforeSentinelData.getHeight() != afterSentinelData.getHeight()
                || beforeSentinelData.getWidth() != afterSentinelData.getWidth()) {
            throw new Exception("Error, classification bands with different sizes");
        }
        Classification svm = Classification.getInstance();
        Raster beforeMask = beforeSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        Raster afterMask = afterSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        int[] beforeMaskPixels = new int[beforeMask.getWidth() * beforeMask.getHeight()];
        beforeMask.getPixels(beforeMask.getMinX(), beforeMask.getMinY(), beforeMask.getWidth(), beforeMask.getHeight(), beforeMaskPixels);
        int[] afterMaskPixels = new int[afterMask.getWidth() * afterMask.getHeight()];
        afterMask.getPixels(afterMask.getMinX(), afterMask.getMinY(), afterMask.getWidth(), afterMask.getHeight(), afterMaskPixels);
        beforeClassificationMatrix = new float[beforeSentinelData.getWidth()][beforeSentinelData.getHeight()];
        afterClassificationMatrix = new float[afterSentinelData.getWidth()][afterSentinelData.getHeight()];
        beforeSentinelData.getPixelVector(0);
        afterSentinelData.getPixelVector(0);
        int width = beforeSentinelData.getWidth();
        int height = beforeSentinelData.getHeight();
        IntStream.range(0, width - 1).parallel().forEach(x ->
            IntStream.range(0, height - 1).parallel().forEach(y -> {
//                System.out.println(x + " " + y);
                int i = x * height + y;
                if (beforeMaskPixels[i] != 1 && beforeMaskPixels[i] != -9999.0
                        && afterMaskPixels[i] != 1 && afterMaskPixels[i] != -9999.0) {
                    beforeClassificationMatrix[x][y] = svm.predict(beforeSentinelData.getPixelVector(i));
                    afterClassificationMatrix[x][y] = svm.predict(afterSentinelData.getPixelVector(i));
                } else {
                    beforeClassificationMatrix[x][y] = -1;
                    afterClassificationMatrix[x][y] = -1;
                }
            })
        );
    }

    void checkAndFixPixels() {
        // Check pixels
        checkPixels(beforeClassificationMatrix);
        checkPixels(afterClassificationMatrix);
    }

    void extractPolygons() {
        // Create classification raster
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D beforeClassesGrid = factory.create("Before classes", beforeClassificationMatrix, beforeSentinelData.getEnvelope());
        GridCoverage2D afterClassesGrid = factory.create("After classes", afterClassificationMatrix, afterSentinelData.getEnvelope());
        // Raster to vector
        final PolygonExtractionProcess process = new PolygonExtractionProcess();
        this.beforeClassification = process.execute(beforeClassesGrid,  0, true,
                null, Collections.singletonList(-1), null, null);
//        System.out.println("Polygon Extraction After");
        this.afterClassification = process.execute(afterClassesGrid, 0, true,
                null, Collections.singletonList(-1), null, null);
    }

    void detectLandUseChanges() throws FactoryException {
        this.changeDetection = getIntersections(this.beforeClassification, this.afterClassification);
        this.changeDetection = Utils.transformChangeDetectionCollectionCRS(this.changeDetection, DefaultGeographicCRS.WGS84);
    }

    void calculateLUCDAreas() throws Exception {
        this.areas = Data.getSquares(this.changeDetection);
    }

    /**
     * Checking for pixel neighbors
     * @param pixels pixels matrix
     */
    private void checkPixels(float[][] pixels) {
        IntStream.range(0, pixels.length - 1).parallel().forEach(x ->
            IntStream.range(0, pixels[x].length - 1).parallel().forEach(y -> {
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
                        if (!intersection.isEmpty()) {
                            intersection.setUserData(geometry);
                            Object beforeClass = feature.getAttribute("value");
                            Object afterClass = afterFeature.getAttribute("value");
                            if (intersection instanceof Polygon || intersection instanceof MultiPolygon) {
                                featureBuilder.add(intersection);
                                featureBuilder.add(beforeClass);
                                featureBuilder.add(afterClass);
                                collection.add(featureBuilder.buildFeature(null));
                            } else if (intersection instanceof GeometryCollection) {
                                GeometryCollection geometryCollection = (GeometryCollection)intersection;
                                for (int i = 0; i < geometryCollection.getNumGeometries(); ++i) {
                                    Geometry geom = geometryCollection.getGeometryN(i);
                                    if (!geom.isEmpty() && (geom instanceof Polygon || geom instanceof MultiPolygon)) {
                                        featureBuilder.add(geom);
                                        featureBuilder.add(beforeClass);
                                        featureBuilder.add(afterClass);
                                        collection.add(featureBuilder.buildFeature(null));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return collection;
    }
}
