package LandUseChangeDetection;

import LandUseChangeDetection.data.Data;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.raster.PolygonExtractionProcess;

import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

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
        for (int x = 0; x < beforeSentinelData.getWidth(); ++x) {
            for (int y = 0; y < beforeSentinelData.getHeight(); ++y) {
                int i = x * beforeSentinelData.getHeight() + y;
                if (beforeMaskPixels[i] != 1 && afterMaskPixels[i] != 1) {
                    beforeClassification[x][y] = svm.predict(beforeSentinelData.getPixelVector(i));
                    afterClassification[x][y] = svm.predict(afterSentinelData.getPixelVector(i));
                } else {
                    beforeClassification[x][y] = -1;
                    afterClassification[x][y] = -1;
                }
            }
        }
        // Check pixels
        checkPixels(beforeClassification);
        checkPixels(afterClassification);
        // Create classification raster
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D beforeClassesGrid = factory.create("Before classes", beforeClassification, beforeSentinelData.getEnvelope());
        GridCoverage2D afterClassesGrid = factory.create("After classes", afterClassification, afterSentinelData.getEnvelope());
        // Raster to vector
        final PolygonExtractionProcess process = new PolygonExtractionProcess();
        SimpleFeatureCollection beforeCollection = process.execute(beforeClassesGrid,  0, true,
                null, null, null, null);
        SimpleFeatureCollection afterCollection = process.execute(afterClassesGrid, 0, true,
                null, null, null, null);
        // Get land use changes
        return Data.getLandUseChanges(beforeCollection, afterCollection);
    }

    /**
     * Checking for pixel neighbors
     * @param pixels pixels matrix
     */
    private void checkPixels(float[][] pixels) {
        for (int x = 0; x < pixels.length; ++x) {
            for (int y = 0; y < pixels.length; ++y) {
                float val = pixels[x][y];
                if (val == -1) {
                    continue;
                }
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
        }
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
