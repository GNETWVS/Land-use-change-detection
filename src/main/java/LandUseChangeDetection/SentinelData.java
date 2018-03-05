package LandUseChangeDetection;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverageio.jp2k.JP2KReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SentinelData {

    private List<GridCoverage2D> bands;

    SentinelData(File imageDir) throws Exception {
        this.bands = new ArrayList<>(12);
        for (int i = 0; i < 12; ++i) {

        }
    }

    static GridCoverage2D openSentinelData(File bandFile) throws IOException {
        JP2KReader reader = new JP2KReader(bandFile);
        return reader.read(null);
    }
}
