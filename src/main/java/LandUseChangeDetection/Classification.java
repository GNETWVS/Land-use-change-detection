package LandUseChangeDetection;

import it.geosolutions.imageio.plugins.jp2k.JP2KKakaduImageWriter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverageio.jp2k.JP2KFormat;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.ConstantExpression;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.util.Converters;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.*;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                    "farm"
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
                    "school"
            )),
            // Landfill
            Collections.unmodifiableList(Arrays.asList(

            )),
            // Forest class
            Collections.unmodifiableList(Arrays.asList(
                    "forest"
            ))
    ));

    private Classification() {
        // TODO: Разобраться с sigma и penalty
        // TODO: Grid seach (c and gamma)
        this.svm = new SVM<double[]>(new GaussianKernel(1), 1, 10, SVM.Multiclass.ONE_VS_ONE);
    }

    public static Classification getInstance() {
        if (instance == null) {
            instance = new Classification();
        }
        return instance;
    }

    void learn(double[][] data, int[] label) {
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
        getNextGISTrainingSamples(nextShp, sData);
    }

    /**
     * NextGIS shapefile with water features
     */
    private final static String WATER_POLYGON_SHP_NAME = "water-polygon.shp";

    /**
     *
     * @param nextShp
     * @param sentinelData
     * @throws Exception
     */
    private void getNextGISTrainingSamples(File nextShp, SentinelData sentinelData) throws Exception {
        // Checking for directory
        if (!nextShp.isDirectory()) {
            throw new Exception("Error, NextSHP file is not directory");
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
        GridCoverage2D waterCoverage = VectorToRasterProcess.process(waterFC, ConstantExpression.constant(1),
                sentinelData.getGridDimension(), sentinelData.getEnvelope(), "waterMask", null);
        
    }
}
