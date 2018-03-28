package LandUseChangeDetection.forms;

import LandUseChangeDetection.Classification;
import LandUseChangeDetection.SentinelData;
import LandUseChangeDetection.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.esa.s2tbx.dataio.VirtualPath;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.stream.Collectors;

public class TrainNextGisForm {

    /**
     * Learn SVM by NextGIS data anchor pane
     */
    public AnchorPane learnNextGISForm;
    public Label esaDataLabel;
    public Button selectGranuleButton;
    public ComboBox granuleSelectionBox;
    public ComboBox resolutionBox;
    public Button selectTrainingVectorButton;
    public Label nextGisVectorFileLabel;
    public Button trainButton;

    /**
     * Training sentinel file
     */
    private File sentinel2ALevelFile = null;

    /**
     * Training granule
     */
    private VirtualPath granulePath = null;

    public void initialize() {
        this.resolutionBox.setItems(resolutions);
        this.resolutionBox.setValue(resolutions.get(0));
    }

    /**
     * Selection of Sentinel training files
     * @param actionEvent Selection action event
     */
    public void selectSentinelTrainingData(ActionEvent actionEvent) {
        FileChooser dc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        this.sentinel2ALevelFile = dc.showOpenDialog(learnNextGISForm.getScene().getWindow());
        if (this.sentinel2ALevelFile == null) {
            esaDataLabel.setText("Data not selected");
            selectGranuleButton.setDisable(true);
        } else {
            esaDataLabel.setText(this.sentinel2ALevelFile.getAbsolutePath());
            selectGranuleButton.setDisable(false);
        }
    }

    /**
     * Training shapefile
     */
    private File trainingShapefile;

    /**
     * Training shapefile selection
     * @param actionEvent action event
     */
    public void selectTrainingVectorFile(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.trainingShapefile = dc.showDialog(learnNextGISForm.getScene().getWindow());
        if (this.trainingShapefile == null) {
            nextGisVectorFileLabel.setText("Data not selected");
            trainButton.setDisable(true);
        } else {
            nextGisVectorFileLabel.setText(this.trainingShapefile.getAbsolutePath());
            trainButton.setDisable(false);
        }
    }

    /**
     * Train SVM model
     * @param actionEvent training action event
     */
    public void trainSVMModel(ActionEvent actionEvent) {
        if (this.sentinel2ALevelFile == null || this.trainingShapefile == null) {

            return;
        }
        File granule = new File(granuleSelectionBox.getValue().toString());
        Classification svm = Classification.getInstance();
        try {
            svm.trainByNextGISData(this.trainingShapefile, granule);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolutions list
     */
    private final static ObservableList<String> resolutions = FXCollections.observableArrayList("10m", "20m", "60m");

    /**
     * Selection granules handler
     * @param actionEvent granule selection action handler
     */
    public void selectGranuleButtonHandler(ActionEvent actionEvent) {
        if (this.sentinel2ALevelFile == null) {
            Utils.showErrorMessage("Sentinel 2 Data Error",
                    "Error, Sentinel data not selected",
                    "");
            return;
        }
        try {
            List<VirtualPath> granules = SentinelData.checkAndGetGranules(this.sentinel2ALevelFile);
            ObservableList<String> granulesList = FXCollections.observableArrayList(
                    granules.stream().map(VirtualPath::getFullPathString)
                    .collect(Collectors.toList()));
            granuleSelectionBox.setItems(granulesList);
            granuleSelectionBox.setValue(granulesList.get(0));
            granuleSelectionBox.setDisable(false);
            resolutionBox.setDisable(false);
            selectTrainingVectorButton.setDisable(false);
        } catch (Exception e) {
            Utils.showErrorMessage("Granules extracting error",
                    "Error, granules extracting ",
                    e.getMessage());
        }
    }
}
