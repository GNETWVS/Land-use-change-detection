package LandUseChangeDetection;

import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverageio.jp2k.JP2KReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel data resolutions
 */
enum Resolution {
    R10m,
    R20m,
    R60m
}

public class SentinelData {

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
        gdal.AllRegister();
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
}
