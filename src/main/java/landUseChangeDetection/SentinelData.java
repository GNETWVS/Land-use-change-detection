package landUseChangeDetection;

import com.vividsolutions.jts.geom.Geometry;
import it.geosolutions.jaiext.JAIExt;
import org.apache.commons.io.FilenameUtils;
import org.esa.s2tbx.dataio.VirtualPath;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2ProductNamingUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.coverageio.jp2k.JP2KReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentinelData {

    static {
        gdal.AllRegister();
        JAIExt.initJAIEXT();
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
    List<GridCoverage2D> getBands() {
        return bands;
    }

    void setBands(List<GridCoverage2D> bands) {
        this.pixels = null;
        this.bands = bands;
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
     * @return grid dimension
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
    double[] getPixelVector(int pixel) {
        // Lazy initialization
        if (pixels == null) {
            pixels = new double[bands.size()][];
            for (int i = 0; i < bands.size(); i++) {
                Raster band = bands.get(i).getRenderedImage().getData();
                pixels[i] = new double[this.getWidth() * this.getHeight()];
                band.getPixels(band.getMinX(), band.getMinY(), this.getWidth(), this.getHeight(), pixels[i]);
            }
        }
        if (this.pixels[0][pixel] < 0) {
            return null;
        }
        double[] vector = new double[bands.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = pixels[i][pixel];
        }
        return vector;
    }

    /**
     * Check product structure
     * @param dataFile Path to product XML
     * @return List of granules
     */
    public static List<VirtualPath> checkAndGetGranules(File dataFile) throws Exception {
        // Check Sentinel 2 data
        VirtualPath xmlPath = new VirtualPath(dataFile.getAbsolutePath());
        if (!S2ProductNamingUtils.checkStructureFromProductXml(xmlPath)) {
            throw new Exception("Error, invalid Sentinel 2 product");
        }
        S2Config.Sentinel2ProductLevel level = S2ProductNamingUtils.getLevel(
                xmlPath, S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA);
        if (level != S2Config.Sentinel2ProductLevel.L2A) {
            throw new Exception("Error, Sentinel 2 level should be Level-2A, not " + level.toString() +
                    " Please, use Level updater");
        }
        if (!S2ProductNamingUtils.hasValidStructure(
                S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA, xmlPath)) {
            throw new Exception("Error, Sentinel 2 Product doesn't have valid structure");
        }
        return S2ProductNamingUtils.getTilesFromProductXml(xmlPath);
    }

    /**
     * Sentinel 2 Data sensing datetime
     */
    private Date sensingDate;

    /**
     * Sensing date getter
     * @return Sentinel 2 sensing datetime
     */
    Date getSensingDate() {
        return this.sensingDate;
    }

    /**
     * Date extracting pattern
     */
    private static final Pattern DATE_PATTERN = Pattern.compile("(.*)_(\\d{8}T\\d{6})(.*)");

    /**
     * Date layout
     */
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    /**
     * Extracting Sentinel 2 data sensing date and time
     * @param granuleDir Sentinel 2 data granule
     * @return sensing datetime
     * @throws Exception if granule is not exist or contains incorrect sensing date
     */
    private static Date getSensingDate(File granuleDir) throws Exception {
        Matcher m = DATE_PATTERN.matcher(granuleDir.getName());
        if (!m.find()) {
            throw new Exception("Error, granule directory doesn't contains sensing date info");
        }
        String sensingDateString = m.group(2);
        return FORMATTER.parse(sensingDateString);
    }

    private ClassificationEnum type;

    public ClassificationEnum getType() {
        return type;
    }

    public static ClassificationEnum getType(File file) throws Exception {
        if (file.getParentFile().getName().startsWith("S2A")) {
            return ClassificationEnum.A;
        } else if (file.getParentFile().getName().startsWith("S2B")) {
            return ClassificationEnum.B;
        }
        throw new Exception("Please select valid Sentinel file");
    }

    SentinelData(File granuleDir, Resolution r, ClassificationEnum type) throws Exception {
        this.type = type;
        this.resolution = r;
        // Get date
        this.sensingDate = getSensingDate(granuleDir);
        // Get bands' files
        StringBuilder fileBuilder = new StringBuilder(granuleDir.getAbsolutePath());
        fileBuilder.append(File.separator);
        fileBuilder.append("IMG_DATA");
        fileBuilder.append(File.separator);
        File imgDataFile = new File(fileBuilder.toString());
        fileBuilder.append(r);
        fileBuilder.append(File.separator);
        File file = new File(fileBuilder.toString());
        // Checking for old scl data
        File[] imgDataDirFiles = imgDataFile.listFiles();
        if (imgDataDirFiles != null && imgDataDirFiles.length > 1) {
            for (File f : imgDataDirFiles) {
                if (f.getName().contains("SCL")) {
                    File target = new File(file.getAbsolutePath() + File.separator + f.getName());
                    if (!target.exists()) {
                        Files.copy(f.toPath(), target.toPath());
                    }
                }
            }
        }
        File[] files = file.listFiles();
        if (files == null) {
            throw new NullPointerException("Error, incorrect SL2 Data");
        }
        // TODO: Filter bands
        // TODO: Interpolation 10m
        bands = new ArrayList<>(files.length);
        for (File bandFile : files) {
            if (FilenameUtils.getExtension(bandFile.getName()).equals(JP2K_EXTENSION)) {
                if (!bandFile.getName().contains("TCI") && !bandFile.getName().contains("AOT")) {
                    bands.add(openSentinelData(bandFile));
                }
            }
        }
        fileBuilder = new StringBuilder(granuleDir.getAbsolutePath());
        fileBuilder.append(File.separator);
        fileBuilder.append("QI_DATA");
        fileBuilder.append(File.separator);
        file = new File(fileBuilder.toString());
        files = file.listFiles();
        if (files == null) {
            throw new NullPointerException("Error, incorrect SL2 Data");
        }
        // TODO: 10m
        String resolutionMarker = r.toString().substring(1);
        for (File qFile : files) {
            if (FilenameUtils.getExtension(qFile.getName()).equals(JP2K_EXTENSION)){
                if (qFile.getPath().endsWith(resolutionMarker + ".jp2") && qFile.getPath().contains("CLD")) {
                    this.cloudsMaskFile = qFile;
                    continue;
                }
                if (qFile.getPath().endsWith(resolutionMarker + ".jp2") && qFile.getPath().contains("SNW")) {
                    this.snowMaskFile = qFile;
                }
            }
        }
        if (this.cloudsMaskFile == null || this.snowMaskFile == null ) {
            throw new Exception("Error, granule not contain quality data");
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(prjFile.getAbsolutePath()))) {
            writer.write(prjStr);
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(j2wFile.getAbsolutePath()))) {
            writer.write(wb.toString());
        }
    }

    /**
     * Crop bands
     * @param envelope cropping envelope
     */
    void cropBands(Envelope envelope) throws Exception {
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
        if (this.cloudAndSnowMask == null) {
            this.getCloudsAndSnowMask();
        }
        params.parameter("Source").setValue(this.cloudAndSnowMask);
        this.cloudAndSnowMask = (GridCoverage2D) processor.doOperation(params);
//        ListIterator<GridCoverage2D> ita = this.bands.listIterator();
//        while (ita.hasNext()) {
//            GridCoverage2D band = ita.next();
//            File file = new File("C:\\Users\\Arthur\\Desktop\\Java\\" + ita.nextIndex() + ".tiff");
//            GeoTiffWriter writer = new GeoTiffWriter(file);
//            writer.write(band, null);
//        }
    }

    void cropBands(Geometry geometry) throws Exception {
        final CoverageProcessor processor = new CoverageProcessor();
        ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
        params.parameter(Crop.PARAMNAME_ROI).setValue(geometry);
        ListIterator<GridCoverage2D> it = this.bands.listIterator();
        while (it.hasNext()) {
            GridCoverage2D band = it.next();
            params.parameter("Source").setValue(band);
            band = (GridCoverage2D) processor.doOperation(params);
            it.set(band);
        }
        if (this.cloudAndSnowMask == null) {
            this.getCloudsAndSnowMask();
        }
        params.parameter("Source").setValue(this.cloudAndSnowMask);
        this.cloudAndSnowMask = (GridCoverage2D) processor.doOperation(params);
//        ListIterator<GridCoverage2D> ita = this.bands.listIterator();
//        while (ita.hasNext()) {
//            GridCoverage2D band = ita.next();
//            File file = new File("C:\\Users\\Arthur\\Desktop\\Java\\" + ita.nextIndex() + ".tiff");
//            GeoTiffWriter writer = new GeoTiffWriter(file);
//            writer.write(band, null);
//        }
    }

    // TODO: comment
    private static GridCoverage2D interpolateData(GridCoverage2D coverage, GridGeometry2D gridGeometry2D) {
        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        return (GridCoverage2D) Operations.DEFAULT.resample(coverage, coverage.getCoordinateReferenceSystem(), gridGeometry2D, interpolation);
    }

    /**
     * Clouds mask file
     */
    private File cloudsMaskFile;

    /**
     * Snow mask file
     */
    private File snowMaskFile;

    /**
     * Low value for excluding values
     */
    private static final Double BORDER_VALUE = 90.0;

    /**
     * Clouds and snow mask
     */
    private GridCoverage2D cloudAndSnowMask;

    /**
     * Get clouds and snow mask
     * @return clouds and snow mask
     */
    public GridCoverage2D getCloudsAndSnowMask() throws Exception {
        if (this.cloudAndSnowMask == null) {
            GridCoverage2D cloudsMask = openSentinelData(cloudsMaskFile);
            GridCoverage2D snowMask = openSentinelData(snowMaskFile);
            // JAI operations
            ParameterBlock maskOp = new ParameterBlock();
            maskOp.addSource(cloudsMask.getRenderedImage());
            maskOp.addSource(snowMask.getRenderedImage());
            RenderedOp cloudAndSnowMask = JAI.create("Or", maskOp);

            ParameterBlock selectOp = new ParameterBlock();
            selectOp.addSource(cloudAndSnowMask);
            selectOp.add(BORDER_VALUE);
            cloudAndSnowMask = JAI.create("Binarize", selectOp);

            GridCoverageFactory factory = new GridCoverageFactory();
            ReferencedEnvelope envelope = new ReferencedEnvelope(cloudsMask.getEnvelope());

            this.cloudAndSnowMask = factory.create("CloudAndSnowMask", cloudAndSnowMask, envelope);
        }
        return this.cloudAndSnowMask;
    }

//    /**
//     * Merge masks
//     * @param first first mask
//     * @param second second mask
//     * @return merged mask
//     * @throws Exception if mask is null or not equals
//     */
//    public static GridCoverage2D mergeMasks(GridCoverage2D first, GridCoverage2D second) throws Exception {
//        if (first == null || second == null) {
//            throw new Exception("Error, mask cannot be null");
//        }
//        Envelope a = first.getEnvelope2D();
//        Envelope b = second.getEnvelope2D();
//        if (first.getEnvelope() != second.getEnvelope()) {
//            //throw new Exception("Error, masks must have equal envelopes");
//        }
//        // JAI operations
//        ParameterBlock maskOp = new ParameterBlock();
//        maskOp.addSource(first.getRenderedImage());
//        maskOp.addSource(second.getRenderedImage());
//        RenderedOp mask = JAI.create("Or", maskOp);
//
//        GridCoverageFactory factory = new GridCoverageFactory();
//        ReferencedEnvelope envelope = new ReferencedEnvelope(first.getEnvelope());
//
//        return factory.create("Mask", mask, envelope);
//    }
}
