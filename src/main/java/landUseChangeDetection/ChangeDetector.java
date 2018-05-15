package landUseChangeDetection;

import landUseChangeDetection.data.Data;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

import javax.media.jai.RasterFactory;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
            throw new Exception("Error, data is null");
        }
        beforeSentinelData.cropBands(afterSentinelData.getEnvelope());
        afterSentinelData.cropBands(beforeSentinelData.getEnvelope());
    }

    private Geometry roi;

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
        this.roi = union;
        beforeSentinelData.cropBands(union);
        afterSentinelData.cropBands(union);
    }

    private Float[][] beforeClassificationMatrix;
    private Float[][] afterClassificationMatrix;

    void certificate() throws Exception {
        if (beforeSentinelData.getHeight() != afterSentinelData.getHeight()
                || beforeSentinelData.getWidth() != afterSentinelData.getWidth()) {
            throw new Exception("Error, classification bands with different sizes");
        }
        Classification beforeSVM = Classification.getInstance(beforeSentinelData.getType());
        Classification afterSVM = Classification.getInstance(afterSentinelData.getType());
        Raster beforeMask = beforeSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        Raster afterMask = afterSentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        int[] beforeMaskPixels = new int[beforeMask.getWidth() * beforeMask.getHeight()];
        beforeMask.getPixels(beforeMask.getMinX(), beforeMask.getMinY(), beforeMask.getWidth(), beforeMask.getHeight(), beforeMaskPixels);
        int[] afterMaskPixels = new int[afterMask.getWidth() * afterMask.getHeight()];
        afterMask.getPixels(afterMask.getMinX(), afterMask.getMinY(), afterMask.getWidth(), afterMask.getHeight(), afterMaskPixels);
        beforeClassificationMatrix = new Float[beforeSentinelData.getWidth()][beforeSentinelData.getHeight()];
        afterClassificationMatrix = new Float[afterSentinelData.getWidth()][afterSentinelData.getHeight()];
        beforeSentinelData.getPixelVector(0);
        afterSentinelData.getPixelVector(0);
        int width = beforeSentinelData.getWidth();
        int height = beforeSentinelData.getHeight();
        IntStream.range(0, width).parallel().forEach(x ->
            IntStream.range(0, height).parallel().forEach(y -> {
                int i = y * width + x;;
                if (beforeMaskPixels[i] != 1 && beforeMaskPixels[i] != -9999 && beforeMaskPixels[i] != Float.NaN
                        && afterMaskPixels[i] != 1 && afterMaskPixels[i] != -9999 && afterMaskPixels[i] != Float.NaN) {
                    double[] bPixels = beforeSentinelData.getPixelVector(i);
                    double[] aPixels = afterSentinelData.getPixelVector(i);
                    if (bPixels != null && aPixels != null) {
                        beforeClassificationMatrix[x][y] = new Float(beforeSVM.predict(bPixels));
                        afterClassificationMatrix[x][y] = new Float(afterSVM.predict(aPixels));
                    } else {
                        beforeClassificationMatrix[x][y] = Float.NaN;
                        afterClassificationMatrix[x][y] = Float.NaN;
                    }
                } else {
                    beforeClassificationMatrix[x][y] = Float.NaN;
                    afterClassificationMatrix[x][y] = Float.NaN;
                }
            })
        );
    }

    void checkAndFixPixels() {
        // Check pixels
        checkPixels(beforeClassificationMatrix);
        checkPixels(afterClassificationMatrix);
    }

    void extractPolygons() throws IOException {
        GridCoverageFactory factory = new GridCoverageFactory();
        WritableRaster before = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT,
                beforeSentinelData.getWidth(), beforeSentinelData.getHeight(), 1, null);
        for (int i = 0; i < beforeClassificationMatrix.length; ++i) {
            Float[] row = beforeClassificationMatrix[i];
            for (int j = 0; j < row.length; ++j) {
                before.setSample(i, j, 0, row[j]);
            }
        }
        WritableRaster after = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT,
                afterSentinelData.getWidth(), afterSentinelData.getHeight(), 1, null);
        for (int i = 0; i < afterClassificationMatrix.length; ++i) {
            Float[] row = afterClassificationMatrix[i];
            for (int j = 0; j < row.length; ++j) {
                after.setSample(i, j, 0, row[j]);
            }
        }
        GridCoverage2D beforeClassesGrid = factory.create("Before classes", before, beforeSentinelData.getEnvelope());
        GridCoverage2D afterClassesGrid = factory.create("After classes", after, afterSentinelData.getEnvelope());
        // Raster to vector
        final PolygonExtractionProcess process = new PolygonExtractionProcess();
        this.beforeClassification = process.execute(beforeClassesGrid,  0, true,
                this.roi, Collections.singletonList(Float.NaN), null, null);
//        System.out.println("Polygon Extraction After");
        this.afterClassification = process.execute(afterClassesGrid, 0, true,
                this.roi, Collections.singletonList(Float.NaN), null, null);
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
    private void checkPixels(Float[][] pixels) {
        IntStream.range(0, pixels.length - 1).parallel().forEach(x ->
            IntStream.range(0, pixels[x].length - 1).parallel().forEach(y -> {
                Float val = pixels[x][y];
                if (val != -1) {
                    List<Float> neighbors = new ArrayList<>();
                    for (int i = x - 1; i <= x + 1; ++i) {
                        for (int j = y - 1; j <= y + 1; ++j) {
                            if ((x == i && y == j)
                                    || i < 0
                                    || j < 0
                                    || i >= pixels.length
                                    || j >= pixels[0].length
                                    || pixels[i][j] == Float.NaN) {
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

    public String getWKT() throws Exception {
        if (this.changeDetection == null) {
            throw new Exception("Error, calculate lucd before");
        }
        StringBuilder builder = new StringBuilder(this.changeDetection.getSchema().getCoordinateReferenceSystem().toWKT());
        builder.append("\n\n");
        SimpleFeatureIterator it = this.changeDetection.features();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            builder.append(beforeSentinelData.getSensingDate().toString());
            builder.append(": ");
            builder.append(getLandUseClass((int)feature.getAttribute("before")));
            builder.append(" ");
            builder.append(afterSentinelData.getSensingDate().toString());
            builder.append(": ");
            builder.append(getLandUseClass((int)feature.getAttribute("after")));
            builder.append(" ");
            builder.append(TopologyPreservingSimplifier.simplify((Geometry) feature.getDefaultGeometry(), 0).toString());
            builder.append("\n");
        }
        return builder.toString();
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

    private static String getLandUseClass(int c) throws Exception {
        if (c == 0) return "water";
        if (c == 1) return "agriculture";
        if (c == 2) return "urban";
        if (c == 3) return "forest";
        throw new Exception("Error, unknown land-land use class");
    }
}
