package LandUseChangeDetection;

import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverageio.jp2k.JP2KReader;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.awt.image.Raster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Sentinel data resolutions
 */
enum Resolution {
    R10m,
    R20m,
    R60m
}

public class SentinelData {

    static {
        gdal.AllRegister();
    }

    /**
     * JP2k Extension
     */
    private static final String JP2K_EXTENSION = "jp2";

    /**
     * Projection file
     */
    private static final String PRJ_EXTENSION = ".prj";

    /**
     * World file
     */
    private static final String WDL_EXTENSION = ".j2w";

    /**
     * Sentinel data bands
     */
    private List<GridCoverage2D> bands;

    /**
     * Bands getter
     * @return list of bands
     */
    public List<GridCoverage2D> getBands() {
        return bands;
    }

    /**
     * Sentinel data resolution
     */
    private Resolution resolution;

    /**
     * Resolution getter
     * @return Bands resolution
     */
    public Resolution getResolution() {
        return resolution;
    }

    /**
     * Get bands CRS
     * @return bands CRS
     */
    public CoordinateReferenceSystem getCRS() {
        if (this.bands == null || this.bands.size() == 0) {
            return null;
        }
        return this.bands.get(0).getCoordinateReferenceSystem2D();
    }

    public int getWidth() {
        return this.bands.get(0).getRenderedImage().getWidth();
    }

    public int getHeight() {
        return this.bands.get(0).getRenderedImage().getHeight();
    }

    /**
     * Get bands' grid dimension
     * @return
     */
    public Dimension getGridDimension() {
        if (this.bands == null || this.bands.size() == 0) {
            return null;
        }
        return new Dimension(this.getWidth(), this.getHeight());
    }

    /**
     * Get raster's envelope
     * @return Raster envelope
     */
    public Envelope getEnvelope() {
        if (this.bands == null || this.bands.size() == 0) {
            return null;
        }
        return this.bands.get(0).getEnvelope();
    }

    /**
     * Data pixels
     */
    private double[][] pixels;

    /**
     * Get pixel data vector
     * @param pixel pixel number
     * @return Data vector
     */
    public double[] getPixelVector(int pixel) {
        // Lazy initialization
        if (pixels == null) {
            pixels = new double[bands.size()][];
            for (int i = 0; i < bands.size(); i++) {
                Raster band = bands.get(i).getRenderedImage().getData();
                pixels[i] = new double[this.getWidth() * this.getHeight()];
                band.getPixels(band.getMinX(), band.getMinY(), this.getWidth(), this.getHeight(), pixels[i]);
            }
        }
        // TODO: Checking for size?
        double[] vector = new double[bands.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = pixels[i][pixel];
        }
        return vector;
    }

    SentinelData(File dataDir, Resolution r) throws Exception {
        this.resolution = r;
        // Get bands' files
        StringBuilder fileBuilder = new StringBuilder(dataDir.getAbsolutePath());
        fileBuilder.append(File.separator);
        fileBuilder.append("GRANULE");
        fileBuilder.append(File.separator);
        File file = new File(fileBuilder.toString());
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            throw new NullPointerException("Error, incorrect SL2 Data");
        }
        file = files[0];
        fileBuilder = new StringBuilder(file.getAbsolutePath());
        fileBuilder.append(File.separator);
        fileBuilder.append("IMG_DATA");
        fileBuilder.append(File.separator);
        fileBuilder.append(r);
        fileBuilder.append(File.separator);
        file = new File(fileBuilder.toString());
        files = file.listFiles();
        if (files == null) {
            throw new NullPointerException("Error, incorrect SL2 Data");
        }
        // TODO: Filter bands
        bands = new ArrayList<>(files.length);
        for (File bandFile : files) {
            if (FilenameUtils.getExtension(bandFile.getName()).equals(JP2K_EXTENSION)) {
                bands.add(openSentinelData(bandFile));
            }
        }
    }

    /**
     * Open Sentinel 2 Data
     * @param bandFile band file
     * @return Band data grid coverage
     * @throws IOException if band file doesn't exists
     */
    private GridCoverage2D openSentinelData(File bandFile) throws Exception {
        // Checking for projection and world file
        if (!checkPRJAndJ2W(bandFile)) {
            writeJP2Info(bandFile);
        }
        // Open jp2k sentinel band
        JP2KReader reader = new JP2KReader(bandFile);
        return reader.read(null);
    }

    /**
     * Check for existing of prj and world file
     * @param bandFile Sentinel band file
     * @return existing
     */
    private boolean checkPRJAndJ2W(File bandFile) {
        String pathWithoutExtension = FilenameUtils.removeExtension(bandFile.getAbsolutePath());
        File prjFile = new File(pathWithoutExtension + PRJ_EXTENSION);
        if (!prjFile.exists()) {
            return false;
        }
        File wdlFile = new File(pathWithoutExtension + WDL_EXTENSION);
        return wdlFile.exists();
    }

    /**
     * Create prj and world file
     * @param bandFile Sentinel band file
     */
    private void writeJP2Info(File bandFile) throws Exception {
        //gdal.AllRegister();
        String[] args = new String[]{bandFile.getAbsolutePath()};
        args = gdal.GeneralCmdLineProcessor(args);
        Dataset hDataSet = gdal.Open(args[0], gdalconstConstants.GA_ReadOnly);
        if (hDataSet == null) {
            throw new Exception("GDALOpen failed, " + gdal.GetLastErrorNo() + " " + gdal.GetLastErrorMsg());
        }
        String fileWithoutExtension = FilenameUtils.removeExtension(bandFile.getAbsolutePath());
        writeProjection(hDataSet, new File(fileWithoutExtension + PRJ_EXTENSION));
        writeWorldFile(hDataSet, new File(fileWithoutExtension + WDL_EXTENSION));
        hDataSet.delete();
    }

    /**
     * Write jp2k projection file
     * @param jp2kDataSet jp2k data set
     * @param prjFile prj file
     * @throws IOException Output stream
     */
    private void writeProjection(Dataset jp2kDataSet, File prjFile) throws IOException {
        String prjStr = jp2kDataSet.GetProjectionRef();
        SpatialReference srs = new SpatialReference(prjStr);
        if (prjStr.length() != 0) {
            String[] prjPrettyWKT = new String[1];
            srs.ExportToPrettyWkt(prjPrettyWKT, 0);
            prjStr = prjPrettyWKT[0];
        }
        srs.delete();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(prjFile.getAbsolutePath()));
            writer.write(prjStr);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Write jp2k world file
     * @param hDataSet Sentinel data set
     * @param j2wFile World file
     * @throws IOException Output
     */
    private void writeWorldFile(Dataset hDataSet, File j2wFile) throws IOException {
        double[] geoTransform = new double[6];
        hDataSet.GetGeoTransform(geoTransform);
        StringBuilder wb = new StringBuilder();
        wb.append(geoTransform[1]);
        wb.append(System.lineSeparator());
        wb.append(geoTransform[2]);
        wb.append(System.lineSeparator());
        wb.append(geoTransform[4]);
        wb.append(System.lineSeparator());
        wb.append(geoTransform[5]);
        wb.append(System.lineSeparator());
        double temp = geoTransform[0] + geoTransform[1] / 2;
        wb.append(temp);
        wb.append(System.lineSeparator());
        temp = geoTransform[3] + geoTransform[5] / 2;
        wb.append(temp);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(j2wFile.getAbsolutePath()));
            writer.write(wb.toString());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Crop bands
     * @param envelope cropping envelope
     */
    public void cropBands(Envelope envelope) {
        final CoverageProcessor processor = new CoverageProcessor();
        ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
        params.parameter("Envelope").setValue(envelope);
        ListIterator<GridCoverage2D> it = this.bands.listIterator();
        while (it.hasNext()) {
            GridCoverage2D band = it.next();
            params.parameter("Source").setValue(band);
            band = (GridCoverage2D) processor.doOperation(params);
            it.set(band);
        }
    }
}
