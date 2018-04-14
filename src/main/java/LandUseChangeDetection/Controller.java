package LandUseChangeDetection;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Controller {

    /**
     * Application form
     */
    public BorderPane appForm;

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
        DirectoryChooser dc = new DirectoryChooser();
        this.beforeSentinelData = dc.showDialog(appForm.getScene().getWindow());
    }

    private File afterSentinelData;

    public void selectAfterData(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.afterSentinelData = dc.showDialog(appForm.getScene().getWindow());
    }

    public void detectChanges(ActionEvent actionEvent) {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        try {
            SentinelData firstSentinelData = new SentinelData(beforeSentinelData, Resolution.R60m);
            SentinelData secondSentinelData = new SentinelData(afterSentinelData, Resolution.R60m);
            ChangeDetector detector = new ChangeDetector(
                    firstSentinelData, secondSentinelData
            );
            detector.getChanges();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
