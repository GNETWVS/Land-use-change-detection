package LandUseChangeDetection;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.ConstantExpression;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import smile.classification.SVM;
import smile.math.kernel.GaussianKernel;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;
import java.util.*;

public class Classification implements Serializable {

    /**
     * Classification singleton instance
     */
    private static Classification instance;

    /**
     * Classification B singleton instance
     */
    private static Classification instanceB;

    /**
     * Path ot serializable object
     */
    public static final File svmModelPath = new File("src/resources/model.svm");

    /**
     * Path ot serializable object
     */
    public static final File svmBModelPath = new File("src/resources/modelB.svm");

    /**
     * SVM A model
     */
    private SVM<double[]> svm;

    /**
     * Classes
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
                    "farmyard",
                    "greenhouse_horticulture"
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
                    "brownfield",
                    "construction",
                    "landfill",
                    "quarry",
                    "salt_pond"
            )),
            // Forest class
            Collections.unmodifiableList(Arrays.asList(
                    "forest",
                    "wood"
            ))
    ));

    /**
     * Private constructor
     * @param svm model
     */
    private Classification(SVM<double[]> svm) {
        this.svm = svm;
    }

    /**
     * SVM classification singleton
     * @return Classification instance
     */
    public static Classification getInstance() {
        if (instance == null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(svmModelPath))) {
                SVM<double[]>svm = (SVM<double[]>) ois.readObject(); // TODO: Рабобраться с ошибкой
                instance = new Classification(svm);
            } catch (ClassNotFoundException | IOException e) {
                instance = new Classification(null);
            }
        }
        return instance;
    }

    public static Classification getInstance(ClassificationEnum type) {
        if (type == ClassificationEnum.A) {
            return getInstance();
        }
        if (instanceB == null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(svmBModelPath))) {
                SVM<double[]>svm = (SVM<double[]>) ois.readObject();
                instanceB = new Classification(svm);
            } catch (ClassNotFoundException | IOException e) {
                instanceB = new Classification(null);
            }
        }
        return instanceB;
    }

    /**
     * Serialize trained SVM model
     */
    private void serializeSVMObject() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(svmModelPath))) {
            oos.writeObject(this.svm);
        }
    }

    /**
     * Serialize trained SVM model
     */
    private void serializeSVMObject(ClassificationEnum type) throws IOException {
        if (type == ClassificationEnum.A) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(svmModelPath))) {
                oos.writeObject(this.svm);
            }
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(svmBModelPath))) {
            oos.writeObject(this.svm);
        }
    }

    int predict(double[] vector) {
        return this.svm.predict(vector);
    }

    public int[] predict(double[][] vectors) {
        return this.svm.predict(vectors);
    }

    private void trainAndValidateModel(SVMData svmData) throws IOException {
        // TODO: Grid search values range
        SVM<double[]> selectedSVM = null;
        double selectedS = 0;
        double selectedC = 0;
        double totalAccuracy = 0;
        double[] classAccuracies = new double[LAND_USE_CLASSES.size()];
        SVMSet[] sets = svmData.getCrossValidationData();
        // Grid search and
        for (double s = 0.5; s <= Math.pow(2, 13); s *= 2) {
            for (double c = 0.5; c <= Math.pow(2, 13); c *= 2) {
                double accuracy = 0;
                double maxAccuracy = 0;
                double[] accuracies = new double[LAND_USE_CLASSES.size()];
                double[] maxAccuracies = new double[LAND_USE_CLASSES.size()];
                SVM<double[]> currentSVM = null;
                // Cross validation
                for (int i = 0; i < sets.length; ++i) {
                    SVM<double[]> svm = new SVM<>(new GaussianKernel(s), c, LAND_USE_CLASSES.size(), SVM.Multiclass.ONE_VS_ALL);
                    for (int j = 0; j < sets.length; ++j) {
                        if (j != i) {
                            svm.learn(sets[j].vectors, sets[j].labels);
                        }
                    }
                    svm.finish();

                    int[] predictions = svm.predict(sets[i].vectors);
                    int[] counts = new int[LAND_USE_CLASSES.size()];
                    int[] sizes = new int[LAND_USE_CLASSES.size()];
                    for (int j = 0; j < predictions.length; ++j) {
                        if (sets[i].labels[j] == predictions[j]) {
                            ++counts[predictions[j]];
                        }
                        ++sizes[sets[i].labels[j]];
                    }

                    int count = 0;
                    for (int val : counts) {
                        count += val;
                    }
                    double currentAccuracy = (double) count / predictions.length;
                    accuracy += currentAccuracy;
                    double[] currentAccuracies = new double[LAND_USE_CLASSES.size()];
                    for (int j = 0; j < accuracies.length; ++j) {
                        currentAccuracies[j] = (double)counts[j] / sizes[j];
                        accuracies[j] += currentAccuracies[j];
                    }
//                    System.out.println((double)wCount / predictions.length);
                    if (currentAccuracy > maxAccuracy) {
                        maxAccuracy = currentAccuracy;
                        maxAccuracies = currentAccuracies;
                        currentSVM = svm;
                    }
                }
                accuracy = accuracy / sets.length;
                for (int i = 0; i < accuracies.length; ++i) {
                    accuracies[i] /= sets.length;
                }
                writer.write("S = " + s + "; C = " + c + "; accuracy = " + accuracy + " " + Arrays.toString(accuracies));
                if (accuracy > totalAccuracy) {
                    totalAccuracy = accuracy;
                    classAccuracies = accuracies;
                    selectedSVM = currentSVM;
                    selectedC = c;
                    selectedS = s;
                }
                writer.write("RES: S = " + selectedS + "; C" + selectedC + "; ac = " + totalAccuracy + " " + Arrays.toString(classAccuracies) + "\n");
                writer.newLine();
                writer.flush();
            }
        }
        System.out.println("c = " + selectedC + "; s" + selectedS + "; ac = " + totalAccuracy + " c: " + Arrays.toString(classAccuracies));
        // Online training
        sets = svmData.getCrossValidationData();
        for (int i = 1; i < sets.length; ++i) {
            selectedSVM.learn(sets[i].vectors, sets[i].labels);
        }
        selectedSVM.finish();
        int[] predictions = selectedSVM.predict(sets[0].vectors);
        int[] counts = new int[LAND_USE_CLASSES.size()];
        int[] sizes = new int[LAND_USE_CLASSES.size()];
        for (int j = 0; j < predictions.length; ++j) {
            if (sets[0].labels[j] == predictions[j]) {
                ++counts[predictions[j]];
            }
            ++sizes[sets[0].labels[j]];
        }
        int count = 0;
        for (int val : counts) {
            count += val;
        }
        double currentAccuracy = (double) count / predictions.length;
        writer.write("" + currentAccuracy);
        this.svm = selectedSVM;
        writer.newLine();
    }

    private void learn(double[][] data, int[] label) {
        svm.learn(data, label);
        svm.finish();
    }

    /**
     * Train svm model by NextGIS shapefiles
     * @param nextShp
     * @param s2DataFile
     * @throws Exception
     */
    public void trainByNextGISData(File nextShp, File s2DataFile) throws Exception {
        SentinelData sData = new SentinelData(s2DataFile, Resolution.R60m);
        GridCoverage2D mask = getNextGISCoverage(nextShp, sData);
        SVMData svmData = getTrainingAndValidationData(sData, mask);
        sData = null;
        trainAndValidateModel(svmData);
        svmData = null;
        serializeSVMObject();
    }

    public void trainByNextGISData(File nextShp, File s2DataFile, ClassificationEnum type, Resolution resolution) throws Exception {
        SentinelData sData = new SentinelData(s2DataFile, resolution);
        GridCoverage2D mask = getNextGISCoverage(nextShp, sData);
        SVMData svmData = getTrainingAndValidationData(sData, mask);
        sData = null;
        trainAndValidateModel(svmData);
        svmData = null;
        serializeSVMObject(type);
    }

    private void trainByNextGISData(SentinelData sData, File nextSHP) throws Exception {
        GridCoverage2D mask = getNextGISCoverage(nextSHP, sData);
        SVMData svmData = getTrainingAndValidationData(sData, mask);
        sData = null;
        trainAndValidateModel(svmData);
        svmData = null;
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

    /**
     * Resterization of NextGIS data for classes
     * @param nextShp NextGIS shapefiles directory
     * @param sentinelData Sentinel 2 data
     * @return Array of masks by groups
     * @throws IOException Cannot open files
     * @throws FactoryException Cannot create feature builder
     * @throws TransformException Cannot change CRS
     */
    private GridCoverage2D getNextGISCoverage(File nextShp, SentinelData sentinelData) throws Exception {
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
        featureCollections[1] = extractClassesFeatures(featureCollections, vegetationFC, "NATURAL", sentinelData.getCRS())[1];

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
            masks[i] = VectorToRasterProcess.process(simpleFeatureCollections[i], ConstantExpression.constant((int)Math.pow(2, i)),
                    sentinelData.getGridDimension(), sentinelData.getEnvelope(), String.valueOf(i), null);
        }

        // Merge masks
        GridCoverage2D last = masks[0];
        for (int i = 1; i < masks.length; ++i) {
            ParameterBlock maskOp = new ParameterBlock();
            maskOp.addSource(last.getRenderedImage());
            maskOp.addSource(masks[i].getRenderedImage());
            RenderedOp op = JAI.create("add", maskOp);
            GridCoverageFactory factory = new GridCoverageFactory();
            ReferencedEnvelope envelope = new ReferencedEnvelope(masks[0].getEnvelope());
            last = factory.create("ClassesMask", op, envelope);

        }
        return last;
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
     * @param mask Classes masks
     * @return Extracted and divided data
     */
    private SVMData getTrainingAndValidationData(SentinelData sentinelData, GridCoverage2D mask) throws Exception {
        SVMData svmData = new SVMData();
        int height = sentinelData.getHeight();
        int width = sentinelData.getWidth();
        float[] maskPixels = new float[height * width];
        float[] availablePixels = new float[height * width];
        // Get cloud and snow mask
        Raster cloudsAndSnowRaster = sentinelData.getCloudsAndSnowMask().getRenderedImage().getData();
        cloudsAndSnowRaster.getPixels(cloudsAndSnowRaster.getMinX(), cloudsAndSnowRaster.getMinY(), width, height, availablePixels);
        // Get classes mask
        Raster maskRaster = mask.getRenderedImage().getData();
        maskRaster.getPixels(maskRaster.getMinX(), maskRaster.getMinY(), width, height, maskPixels);
        for (int i = 0; i < maskPixels.length; ++i) {
            int c = (int)maskPixels[i];
            if (availablePixels[i] == 0.0F && c > 0 && c <= Math.pow(2, LAND_USE_CLASSES.size())) {
                double[] vector = sentinelData.getPixelVector(i);
                if (vector == null) continue;
                for (int j = 0; j < LAND_USE_CLASSES.size(); ++j) {
                    if (Math.pow(2, j) == c) {
                        svmData.add(new SVMVector(vector, j));
                        break;
                    }
                }
            }
        }
        return svmData;
    }

    private class SVMVector{
        double[] vector;
        int label;

        SVMVector(double[] vector, int label) {
            this.vector = vector;
            this.label = label;
        }
    }

    /**
     * SVM training or validation set
     */
    private class SVMSet{
        double[][] vectors;
        int[] labels;

        SVMSet(double[][] vectors, int[] labels) {
            this.vectors = vectors;
            this.labels = labels;
        }
    }


    /**
     * SVM training and validation data
     */
    private class SVMData{

        /**
         * List of data
         */
        private List<SVMVector> data;

        /**
         * SVM data initializer
         */
        SVMData() {
            data = new LinkedList<>();
        }

        /**
         * Add vector and class label
         * @param data data label
         */
        void add(SVMVector data) {
            this.data.add(data);
        }

        /**
         * Get training and validation data in relation 20 : 80
         * @return Training and validation sets
         */
        SVMSet[] getCrossValidationData() {
            SVMSet[] sets = new SVMSet[5];
            for (int i = 0; i < sets.length; ++i) {
                int size;
                if (this.data.size() < 1000) {
                    size = this.data.size();
                } else {
                    size = 1000;
                }
                double[][] tempVectors = new double[size][];
                int[] tempLabels = new int[size];
                for (int j = 0; j < size; ++j) {
                    SVMVector data = this.data.remove(random.nextInt(this.data.size()));
                    tempVectors[j] = data.vector;
                    tempLabels[j] = data.label;
                }
                sets[i] = new SVMSet(tempVectors, tempLabels);
            }
            return sets;
        }
    }

    static File result = new File("C:\\Users\\lukin\\Desktop\\res.txt");
    static BufferedWriter writer;

//    static {
//        try {
//            writer = new BufferedWriter(new FileWriter(result));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    static void findBestBandsSet(File nextShp, File s2DataFile) throws Exception {
        SentinelData data = new SentinelData(s2DataFile, Resolution.R60m);
        List<GridCoverage2D> bands = data.getBands();
        List<List<Integer>> sets = new ArrayList<>();
        for (int i = 0; i < (1 << bands.size()); ++i) {
            sets.add(new ArrayList<>());
            for (int j = 0; j < bands.size(); ++j) {
                if ((i & (1 << j)) != 0) {
                    sets.get(i).add(j);
                }
            }
        }
        sets = Lists.reverse(sets);
        for (List<Integer> list : sets) {
            if (list.size() < 10) {
                continue;
            }
            List<GridCoverage2D> currentBands = new ArrayList<>();
            for (Integer i : list) {
                currentBands.add(bands.get(i));
            }
            data.setBands(currentBands);
            writer.write(Arrays.toString(list.toArray()));
            writer.newLine();
            writer.flush();
            Classification classification = new Classification(null);
            classification.trainByNextGISData(data, nextShp);
        }
        writer.close();
    }
}
