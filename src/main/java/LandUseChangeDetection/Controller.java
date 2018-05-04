package LandUseChangeDetection;

import LandUseChangeDetection.forms.ProgressForm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jdk.nashorn.api.scripting.JSObject;
import org.esa.s2tbx.dataio.VirtualPath;
import org.geotools.data.DataStore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {

    /**
     * Application form
     */
    public BorderPane appForm;
    public ComboBox beforeGranulesComboBox;
    public ComboBox afterGranulesComboBox;
    public WebView webMap;

    /**
     * Data resolutions
     */
    private final ObservableList<String> resolutions = FXCollections.observableArrayList( "60m", "20m");
    public ComboBox resolutionBox;
    public Label waterLabel;
    public Label buildLevel;
    public Label forestLabel;
    public Label waLabel;
    public Label wbLabel;
    public Label wfLabel;
    public Label awLabel;
    public Label abLabel;
    public Label afLabel;
    public Label bwLabel;
    public Label baLabel;
    public Label bfLabel;
    public Label faLabel;
    public Label fwLabel;
    public Label fbLabel;
    public Label agriLabel;
    public Label roiLabel;

    WebEngine webEngine;

    @FXML
    void initialize(){
        resolutionBox.setItems(resolutions);
        resolutionBox.setValue("60m");
        this.webEngine = webMap.getEngine();
        File mapIndexFile = new File("src/resources/AppWebForm/index.html");
        webEngine.load("file:" + mapIndexFile.getAbsolutePath());
        webEngine.setJavaScriptEnabled(true);
    }

    /**
     * Show level up form
     * @param actionEvent level up form opening action event
     */
    public void getLevelUpFormHandler(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/levelUpForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Level Up Sentinel 2 Data");
            stage.setScene(new Scene(root, 600, 270));
            stage.setResizable(false);
            stage.getIcons().add(new Image("/icon.png"));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openDownloadingForm(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/searchAndDownloadForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Search and Download Sentinel 2 Data");
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setScene(new Scene(root, 800, 600));
            stage.getIcons().add(new Image("/icon.png"));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open learn form based on OSM data
     * @param actionEvent OSM learn form opening action event
     */
    public void openLearnOSMForm(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/learnOSMForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Learn SVM by OSM");
            stage.setScene(new Scene(root, 450, 450));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open learn form based on NextGis data
     * @param actionEvent NextGis learn form opening action event
     */
    public void openNextGisLearningForm(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/learnNextGISForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Learn SVM by NextGIS");
             stage.setScene(new Scene(root, 450, 450));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private File beforeSentinelData;

    public void selectBeforeData(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fc.setSelectedExtensionFilter(filter);
        this.beforeSentinelData = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.beforeSentinelData == null) {
            Utils.showErrorMessage("Before Sentinel 2 file is not selected",
                    "Before Sentinel 2 file is not selected",
                    "Please, select Sentinel 2 data file");
        } else {
            try {
                List<VirtualPath> granules = SentinelData.checkAndGetGranules(this.beforeSentinelData);
                ObservableList<String> granulesList = FXCollections.observableArrayList(
                        granules.stream().map(VirtualPath::getFullPathString)
                                .collect(Collectors.toList()));
                beforeGranulesComboBox.setItems(granulesList);
                beforeGranulesComboBox.setValue(granulesList.get(0));
            } catch (Exception e) {
                Utils.showErrorMessage("Error",
                        e.getMessage(),
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private File afterSentinelData;

    public void selectAfterData(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fc.setSelectedExtensionFilter(filter);
        this.afterSentinelData = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.afterSentinelData == null) {
            Utils.showErrorMessage("After Sentinel 2 file is not selected",
                    "After Sentinel 2 file is not selected",
                    "Please, select Sentinel 2 data file");
        } else {
            try {
                List<VirtualPath> granules = SentinelData.checkAndGetGranules(this.afterSentinelData);
                ObservableList<String> granulesList = FXCollections.observableArrayList(
                        granules.stream().map(VirtualPath::getFullPathString)
                                .collect(Collectors.toList()));
                afterGranulesComboBox.setItems(granulesList);
                afterGranulesComboBox.setValue(granulesList.get(0));
            } catch (Exception e) {
                Utils.showErrorMessage("Error",
                        e.getMessage(),
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private ChangeDetector lucd;

    public void detectChanges(ActionEvent actionEvent) {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        try {
            File beforeSentinelGranuleFile = new File(beforeGranulesComboBox.getValue().toString());
            File afterSentinelGranuleFile = new File(afterGranulesComboBox.getValue().toString());
            final Resolution resolution = resolutionBox.getValue().equals("60m") ? Resolution.R60m : Resolution.R20m;

            ProgressForm form = new ProgressForm();
            Task<ChangeDetector> task = new Task<ChangeDetector>() {
                @Override
                protected ChangeDetector call() throws Exception {
                    updateProgress(0, 1);
                    updateMessage("Opening and parsing of " + beforeSentinelGranuleFile.getName());
                    SentinelData firstSentinelData = new SentinelData(beforeSentinelGranuleFile, resolution);
                    updateProgress(0.1, 1);
                    updateMessage("Opening and parsing of " + afterSentinelGranuleFile.getName());
                    SentinelData secondSentinelData = new SentinelData(afterSentinelGranuleFile, resolution);
                    updateProgress(0.2, 1);
                    updateMessage("Bands cropping...");
                    ChangeDetector detector;
                    if (roiFile == null) {
                        detector = new ChangeDetector(firstSentinelData, secondSentinelData);
                    } else {
                        DataStore store = Utils.openShapefile(roiFile);
                        if (store == null || store.getTypeNames() == null || store.getTypeNames().length == 0){
                            throw new NullPointerException("ROI vector data store is null");
                        }
                        String waterTypeName = store.getTypeNames()[0];
                        detector = new ChangeDetector(firstSentinelData, secondSentinelData,
                                Utils.openShapefile(roiFile).getFeatureSource(waterTypeName).getFeatures());
                    }
                    updateProgress(0.3, 1);
                    updateMessage("Bands classification and clouds and snow removing...");
                    detector.certificate();
                    updateProgress(0.5, 1);
                    updateMessage("Land-use classification results checking and fixing...");
                    detector.checkAndFixPixels();
                    updateProgress(0.6, 1);
                    updateMessage("Land-use classification vector extraction...");
                    detector.extractPolygons();
                    updateProgress(0.7, 1);
                    updateMessage("Land-use change detection...");
                    detector.detectLandUseChanges();
                    updateProgress(0.8, 1);
                    updateMessage("Land-use change areas calculation...");
                    detector.calculateLUCDAreas();
                    updateProgress(0.9, 1);

                    return detector;
                }
            };
            form.activateProgressBar(task);
            task.setOnSucceeded(event -> {
                try {
                    this.lucd = task.getValue();
                    List<LandUseChangeDetectionResult> areas = lucd.getAreas();
                    for (LandUseChangeDetectionResult area : areas) {
                        switch (area.getBefore()) {
                            case 0: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        waterLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 1: {
                                        waLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 2: {
                                        wbLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 3: {
                                        wfLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 1: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        awLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 1: {
                                        agriLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 2: {
                                        abLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 3: {
                                        afLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 2: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        bwLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 1: {
                                        baLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 2: {
                                        buildLevel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 3: {
                                        bfLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 3: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        fwLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 1: {
                                        faLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 2: {
                                        fbLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                    case 3: {
                                        forestLabel.textProperty().setValue(area.getArea() + " m\u2072");
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    Utils.writeGeoJSON(this.lucd.getChangeDetection(), "src/resources/AppWebForm/res/result.json");
                    this.webEngine.executeScript("showResult();");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    form.getDialogStage().close();
                }
            });
            new Thread(task).start();
        } catch (Exception e) {
            Utils.showErrorMessage("Error",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
        }
    }

    private File roiFile;

    public void selectROI(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Shapefile (*.shp)", "*.shp");
        fc.setSelectedExtensionFilter(filter);
        this.roiFile = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.roiFile == null) {
            roiLabel.setText("");
        } else {
            roiLabel.setText(roiFile.getAbsolutePath());
        }
    }
}