package LandUseChangeDetection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import it.geosolutions.imageio.plugins.jp2k.JP2KKakaduImageWriter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverageio.jp2k.JP2KFormat;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.ConstantExpression;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.*;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import smile.classification.SVM;
import smile.data.AttributeDataset;
import smile.math.kernel.GaussianKernel;
import sun.plugin2.message.Conversation;

import java.awt.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class Classification {

    /**
     * Classification singleton instance
     */
    private static Classification instance;

    private SVM<double[]> svm;

    /**
     *
     */
    private static final List<List<String>> LAND_USE_CLASSES = Collections.unmodifiableList(Arrays.asList(
            // Water class
            Collections.unmodifiableList(Arrays.asList(
                    "water" // natural
            )),
            // Agriculture
            Collections.unmodifiableList(Arrays.asList(
                    "farmland",
                    "meadow",
                    "orchard",
                    "plant_nursery",
                    "vineyard",
                    "farm",
                    "allotments",
                    "farmyard"
                    //greenhouse_horticulture ??? or urban
            )),
            // Urban classes
            // Build up
            Collections.unmodifiableList(Arrays.asList(
                    "commercial",
                    "garages",
                    "industrial",
                    "religious",
                    "residential",
                    "retail",
                    "school",
//            )),
//            // TODO: Infrasturcure
//            // TODO: Recreation
//            // Landfill
//            Collections.unmodifiableList(Arrays.asList(
                    "brownfield",
                    "construction",
                    "landfill", //(separate?)
                    "quarry",
                    "salt_pond"
            )),
            // Forest class
            Collections.unmodifiableList(Arrays.asList(
                    "forest",
                    "wood" //logging
            ))
    ));

    private Classification() {
        // TODO: Разобраться с sigma и penalty
        // TODO: Grid seach (c and gamma)
        this.svm = new SVM<double[]>(new GaussianKernel(8.0), 5.0, 4, SVM.Multiclass.ONE_VS_ALL);
    }

    public static Classification getInstance() {
        if (instance == null) {
            instance = new Classification();
        }
        return instance;
    }

    private void trainAndValidateModel(SVMData svmData) {
        double[][] data = svmData.getTrainingVectors();
        int[] cl = svmData.getTrainingLabels();
        learn(data, cl);
        double[][] validationData = svmData.getValidationVectors();
        int[] validationInputs = svmData.getValidationLabels();
        int count = 0;
        for (int i = 0; i < data.length; i++){
            int res = svm.predict(data[i]);
            if (res == cl[i]) {
                count++;
                System.out.println("COOL: " + res + " is " + cl[i]);
            } else {
                System.out.println("MISS: " + res + " not " +cl[i]);
            }
        }
        System.out.println((double)count * 100 / validationData.length);
    }

    private void learn(double[][] data, int[] label) {
        svm.learn(data, label);
        svm.finish();
    }

    void getOSMTrainingSamples(File osmShp) throws IOException {
        DataStore shpDataStore = Utils.openShapefile(osmShp);
        for (List<String> classTags : LAND_USE_CLASSES) {
            // TODO: Extract features
            // TODO: Raster mask
        }
    }

    public void trainByNextGISData(File nextShp, File s2DataFile) throws Exception {
        SentinelData sData = new SentinelData(s2DataFile, Resolution.R60m);
        GridCoverage2D[] masks = getNextGISCoverage(nextShp, sData);
        SVMData svmData = getTrainingAndValidationData(sData, masks);
        trainAndValidateModel(svmData);
    }

    /**
     * NextGIS shapefile with water features
     */
    private final static String WATER_POLYGON_SHP_NAME = "water-polygon.shp";

    /**
     * NextGIS shapefile with lu (agriculture)
     */
    private final static String LAND_USE_POLYGON_SHP_NAME = "landuse-polygon.shp";

    /**
     * NextGIS shapefile with vegetation (forest, wood)
     */
    private final static String VEGETATION_POLYGON_SHP_NAME = "vegetation-polygon.shp";

    // TODO: railway and highway

    /**
     * Resterization of NextGIS data for classes
     * @param nextShp NextGIS shapefiles directory
     * @param sentinelData Sentinel 2 data
     * @return Array of masks by groups
     * @throws IOException Cannot open files
     * @throws FactoryException Cannot create feature builder
     * @throws TransformException Cannot change CRS
     */
    private GridCoverage2D[] getNextGISCoverage(File nextShp, SentinelData sentinelData) throws IOException, FactoryException, TransformException {
        // Checking for directory
        if (!nextShp.isDirectory()) {
            throw new FileNotFoundException("Error, NextSHP file is not directory");
        }

        // Water class (water-polygon.shp)
        File waterShpFile = new File(nextShp.getAbsolutePath() + File.separator + WATER_POLYGON_SHP_NAME);
        if (!waterShpFile.exists()) {
            throw new FileNotFoundException("Error, NextGIS doesn't contain water file");
        }
        DataStore waterDataStore = Utils.openShapefile(waterShpFile);
        if (waterDataStore == null || waterDataStore.getTypeNames() == null || waterDataStore.getTypeNames().length == 0){
            throw new NullPointerException("Water vector data store is null");
        }
        String waterTypeName = waterDataStore.getTypeNames()[0];
        SimpleFeatureSource waterFeatureSource = waterDataStore.getFeatureSource(waterTypeName);
        SimpleFeatureCollection waterFC = waterFeatureSource.getFeatures();
        waterFC = Utils.transformToCRS(waterFC, sentinelData.getCRS());


        // Land use shapefile
        File landUseFile = new File(nextShp.getAbsolutePath() + File.separator + LAND_USE_POLYGON_SHP_NAME);
        if (!landUseFile.exists()) {
            throw new FileNotFoundException("Error, NextGIS doesn't contain land-use file");
        }
        DataStore landUseDataStore = Utils.openShapefile(landUseFile);
        if (landUseDataStore == null || landUseDataStore.getTypeNames() == null ||landUseDataStore.getTypeNames().length == 0) {
            throw new NullPointerException("Land-Use vector store is null");
        }
        String landUseTypeName = landUseDataStore.getTypeNames()[0];
        SimpleFeatureSource landUseFeatureSource = landUseDataStore.getFeatureSource(landUseTypeName);
        SimpleFeatureCollection landUseFC = landUseFeatureSource.getFeatures();
        // Extract tags from land use shapefile
        DefaultFeatureCollection[] featureCollections = extractClassesFeatures(null, landUseFC, "LANDUSE", sentinelData.getCRS());

        // Vegetation shape file
        File vegetationFile = new File(nextShp.getAbsolutePath() + File.separator + VEGETATION_POLYGON_SHP_NAME);
        if (!landUseFile.exists()) {
            throw new FileNotFoundException("Error, NextGIS doesn't contain vegetation file");
        }
        DataStore vegetationDataStore = Utils.openShapefile(vegetationFile);
        if (vegetationDataStore == null || vegetationDataStore.getTypeNames() == null || vegetationDataStore.getTypeNames().length == 0) {
            throw new NullPointerException("Vegetation vector store is null");
        }
        String vegetationTypeName = vegetationDataStore.getTypeNames()[0];
        SimpleFeatureSource vegetationFeatureSource = vegetationDataStore.getFeatureSource(vegetationTypeName);
        SimpleFeatureCollection vegetationFC = vegetationFeatureSource.getFeatures();
        // Convert
        featureCollections = extractClassesFeatures(featureCollections, vegetationFC, "NATURAL", sentinelData.getCRS());

        // Convert result to simple feature collection array
        SimpleFeatureCollection[] simpleFeatureCollections = new SimpleFeatureCollection[featureCollections.length];
        System.arraycopy(featureCollections, 0, simpleFeatureCollections, 0, featureCollections.length);

        // Resterization
        GridCoverage2D[] masks = new GridCoverage2D[simpleFeatureCollections.length];
        // Water mask
        GridCoverage2D waterCoverage = VectorToRasterProcess.process(waterFC, ConstantExpression.constant(1),
                sentinelData.getGridDimension(), sentinelData.getEnvelope(), "waterMask", null);
        masks[0] = waterCoverage;
        for (int i = 1; i < masks.length; i++) {
            masks[i] = VectorToRasterProcess.process(simpleFeatureCollections[i], ConstantExpression.constant(1),
                    sentinelData.getGridDimension(), sentinelData.getEnvelope(), String.valueOf(i), null);
        }

        return masks;
    }

    /**
     * Feature type builder
     */
    private SimpleFeatureTypeBuilder typeBuilder = null;

    /**
     * Feature builder
     */
    private SimpleFeatureBuilder featureBuilder = null;

    /**
     * Extract features from collection according croup tags
     * @param featureCollections Collections for updating
     * @param fc Collection with new features
     * @param attributeName Attribute of feature for looking
     * @param crs Purposed coordinate reference system
     * @return Updated feature collections
     * @throws FactoryException Cannot create builder
     * @throws TransformException Cannot transform features
     */
    private DefaultFeatureCollection[] extractClassesFeatures(DefaultFeatureCollection[] featureCollections,
                                                             SimpleFeatureCollection fc, String attributeName,
                                                             CoordinateReferenceSystem crs) throws FactoryException, TransformException {
        // Type builder
        if (this.typeBuilder == null) {
            typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName("Training");
            typeBuilder.setCRS(crs);
            typeBuilder.add("geom", MultiPolygon.class);
            SimpleFeatureType featureType = typeBuilder.buildFeatureType();
            featureBuilder = new SimpleFeatureBuilder(featureType);
        }
        // Init collection array
        if (featureCollections == null) {
            featureCollections = new DefaultFeatureCollection[LAND_USE_CLASSES.size()];
            for (int i = 0; i < featureCollections.length; i++) {
                featureCollections[i] = new DefaultFeatureCollection(null, null);
            }
        }

        // CRS transformer
        MathTransform transform = null;
        if (!CRS.equalsIgnoreMetadata(fc.getSchema().getCoordinateReferenceSystem(), crs)) {
            transform = CRS.findMathTransform(fc.getSchema().getCoordinateReferenceSystem(), crs, true);
        }
        // Extract features
        try (SimpleFeatureIterator it = fc.features()) {
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                String featureTag = (String) (feature.getAttribute(attributeName));
                for (int i = 0; i < LAND_USE_CLASSES.size(); i++) {
                    if (LAND_USE_CLASSES.get(i).contains(featureTag)) {
                        Geometry geometry = (Geometry) feature.getDefaultGeometry();
                        if (transform != null) {
                            geometry = JTS.transform(geometry, transform);
                        }
                        featureBuilder.add(geometry);
                        SimpleFeature tagFeature = featureBuilder.buildFeature(null);
                        featureCollections[i].add(tagFeature);
                        break;
                    }
                }
            }
        }

        return featureCollections;
    }

    /**
     * Random generator
     */
    private static Random random = new Random();

    /**
     * Extract training data and divide to training and validation data
     * @param sentinelData Sentinel 2 Data
     * @param masks Classes masks
     * @return Extracted and divided data
     */
    private SVMData getTrainingAndValidationData(SentinelData sentinelData, GridCoverage2D[] masks) {
        SVMData svmData = new SVMData();
        int height = sentinelData.getHeight();
        int width = sentinelData.getWidth();
        for (int i = 0; i < masks.length; i++) {
            int count = 0;
            float[] maskPixels = new float[height * width];
            Raster mask = masks[i].getRenderedImage().getData();
            mask.getPixels(mask.getMinX(), mask.getMinY(), width, height, maskPixels);
            for (int j = 0; j < maskPixels.length; j++) {
                if (maskPixels[j] == 1.0F) {
                    double[] vector = sentinelData.getPixelVector(j);
                    // Add vector to SVM data
                    if (random.nextBoolean()) {
                        if (++count == 10000) {
                            break;
                        }
                        svmData.addTrainingData(vector, i);
                    } else {
                        svmData.addValidationData(vector, i);
                    }
                }
            }
        }
        return svmData;
    }


    /**
     * SVM training and validation data
     */
    private class SVMData{

        /**
         * List of training Sentinel 2 data vectors
         */
        private List<double[]> trainingVectors;

        /**
         * List of classes of training Sentinel 2 data vectors
         */
        private List<Integer> trainingClasses;

        /**
         * List of validation Sentinel 2 data vectors
         */
        private List<double[]> validationVectors;

        /**
         * List of classes of validation Sentinel 2 data vectors
         */
        private List<Integer> validationClasses;

        double[][] getTrainingVectors() {
            return this.trainingVectors.toArray(new double[trainingVectors.size()][]);
        }

        int[] getTrainingLabels() {
            return this.trainingClasses.stream().mapToInt(i -> i).toArray();
        }

        double[][] getValidationVectors() {
            return this.validationVectors.toArray(new double[validationClasses.size()][]);
        }

        int[] getValidationLabels() {
            return this.validationClasses.stream().mapToInt(i -> i).toArray();
        }

        /**
         * SVM training and validation data initializer
         */
        SVMData() {
            trainingVectors = new ArrayList<>();
            trainingClasses = new ArrayList<>();
            validationVectors = new ArrayList<>();
            validationClasses = new ArrayList<>();
        }

        /**
         * Add training data
         * @param value Sentinel 2 values vector
         * @param cl Data class
         */
        void addTrainingData(double[] value, int cl) {
            this.trainingVectors.add(value);
            this.trainingClasses.add(cl);
        }

        /**
         * Add validation data
         * @param value Sentinel 2 values vector
         * @param cl Data class
         */
        void addValidationData(double[] value, int cl) {
            this.validationVectors.add(value);
            this.validationClasses.add(cl);
        }
    }
}
