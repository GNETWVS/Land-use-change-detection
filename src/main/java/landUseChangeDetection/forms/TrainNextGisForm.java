package landUseChangeDetection.forms;

import landUseChangeDetection.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.esa.s2tbx.dataio.VirtualPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
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
    public Button exportAButton;
    public Button exportBButton;
    public Button cancelButton;
    public Button importAButton;
    public Button importBButton;
    public Button trainingSentinelDataChooser;

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
        dc.setSelectedExtensionFilter(filter);
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
            Utils.showErrorMessage("Error",
                    "Please, choose files for training",
                    "");
            return;
        }
        File granule = new File(granuleSelectionBox.getValue().toString());
        ClassificationEnum type;
        if (sentinel2ALevelFile.getParentFile().getName().startsWith("S2A")) {
            type = ClassificationEnum.A;
        } else if (sentinel2ALevelFile.getParentFile().getName().startsWith("S2B")) {
            type = ClassificationEnum.B;
        } else {
            Utils.showErrorMessage("Error",
                    "Plese select valid Sentinel file",
                    sentinel2ALevelFile.getParentFile().getName());
            return;
        }
        Resolution resolution;
        if (resolutionBox.getValue().equals("60m")) {
            resolution = Resolution.R60m;
        } else {
            resolution = Resolution.R20m;
        }
        Classification svm = Classification.getInstance(type);
        Task task = new Task() {
            @Override
            protected Object call() {
                try {
                    trainingSentinelDataChooser.setDisable(true);
                    selectGranuleButton.setDisable(true);
                    granuleSelectionBox.setDisable(true);
                    resolutionBox.setDisable(true);
                    selectTrainingVectorButton.setDisable(true);
                    importAButton.setDisable(true);
                    importBButton.setDisable(true);
                    exportAButton.setDisable(true);
                    exportBButton.setDisable(true);
                    trainButton.setDisable(true);
                    cancelButton.setDisable(false);
                    svm.trainByNextGISData(trainingShapefile, granule, type, resolution);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Training process finished successfully");
                    alert.showAndWait();
                } catch (Exception e) {
                    Utils.showErrorMessage("Error",
                            e.getMessage(),
                            Arrays.toString(e.getStackTrace()));
                }
                return null;
            }
        };
        this.cancelButton.setOnMouseClicked(event -> {
            task.cancel();
            trainingSentinelDataChooser.setDisable(false);
            selectGranuleButton.setDisable(false);
            granuleSelectionBox.setDisable(false);
            resolutionBox.setDisable(false);
            selectTrainingVectorButton.setDisable(false);
            cancelButton.setDisable(true);
            importAButton.setDisable(false);
            importBButton.setDisable(false);
            exportAButton.setDisable(false);
            exportBButton.setDisable(false);
            trainButton.setDisable(false);
        });

        ((Stage)learnNextGISForm.getScene().getWindow()).setOnHiding(event -> task.cancel());

        new Thread(task).start();
    }

    /**
     * Resolutions list
     */
    private final static ObservableList<String> resolutions = FXCollections.observableArrayList("60m", "20m");

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

    public void exportAHandler(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export SVM A Model");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SVM files (*.svm)", "*.svm");
        fileChooser.setSelectedExtensionFilter(filter);
        File file = fileChooser.showSaveDialog(learnNextGISForm.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(Classification.svmModelPath.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Utils.showErrorMessage("Exporting error",
                        "Cannot export SVM A Model",
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public void exportBHandler(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export SVM B Model");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SVM files (*.svm)", "*.svm");
        fileChooser.setSelectedExtensionFilter(filter);
        File file = fileChooser.showSaveDialog(learnNextGISForm.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(Classification.svmBModelPath.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Utils.showErrorMessage("Exporting error",
                        "Cannot export SVM B Model",
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public void importAHandler(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import SVM A Model");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SVM files (*.svm)", "*.svm");
        fileChooser.setSelectedExtensionFilter(filter);
        File file = fileChooser.showOpenDialog(learnNextGISForm.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(file.toPath(), Classification.svmModelPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import");
                alert.setHeaderText("SVM A model imported successfully. Please, restart program for correct work");
                alert.showAndWait();
            } catch (IOException e) {
                Utils.showErrorMessage("Importing error",
                        "Cannot import SVM A Model",
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public void importBHandler(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import SVM B Model");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SVM files (*.svm)", "*.svm");
        fileChooser.setSelectedExtensionFilter(filter);
        File file = fileChooser.showOpenDialog(learnNextGISForm.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(file.toPath(), Classification.svmBModelPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import");
                alert.setHeaderText("SVM B model imported successfully. Please, restart program for correct work");
                alert.showAndWait();
            } catch (IOException e) {
                Utils.showErrorMessage("Exporting error",
                        "Cannot import SVM B Model",
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
