package LandUseChangeDetection;

import LandUseChangeDetection.forms.SearchAndDownloadForm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import netscape.javascript.JSObject;
import org.esa.s2tbx.dataio.VirtualPath;

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

    @FXML
    void initialize(){
        WebEngine webEngine = webMap.getEngine();
        File mapIndexFile = new File("src/resources/AppWebForm/index.html");
        webEngine.load("file:" + mapIndexFile.getAbsolutePath());
//        webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
//            if (newState == Worker.State.SUCCEEDED) {
//                JSObject window = (JSObject) webEngine.executeScript("window");
//                window.setMember("app", new SearchAndDownloadForm.DownloadAndSearchApplication());
//            }
//        });
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
            stage.setScene(new Scene(root, 600, 800));
            stage.setMaximized(true);
            stage.setResizable(false);
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

    public void detectChanges(ActionEvent actionEvent) {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        try {
            File beforeSentinelGranuleFile = new File(beforeGranulesComboBox.getValue().toString());
            File afterSentinelGranuleFile = new File(afterGranulesComboBox.getValue().toString());
            SentinelData firstSentinelData = new SentinelData(beforeSentinelGranuleFile, Resolution.R60m);
            SentinelData secondSentinelData = new SentinelData(afterSentinelGranuleFile, Resolution.R60m);
            ChangeDetector detector = new ChangeDetector(
                    firstSentinelData, secondSentinelData
            );
            detector.getChanges();
        } catch (Exception e) {
            Utils.showErrorMessage("Error",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
        }
    }
}