package LandUseChangeDetection;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.esa.s2tbx.dataio.s2.Sentinel2ProductReader;
import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProductReader;

import java.io.File;
import java.io.IOException;

public class Controller {

    public AnchorPane osmTrainingPane;

    public void getLevelUpFormHandler(ActionEvent actionEvent) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/levelUpForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Level Up Sentinel 2 Data");
            stage.setScene(new Scene(root, 450, 450));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public void openNextGisLearningForm(ActionEvent actionEvent) {
    }

    /**
     * Training sentinel data
     */
    private File sentinelTrainingFile = null;

    /**
     *
     * @param actionEvent
     */
    public void selectSentinelTrainingData(ActionEvent actionEvent) {
        //Select file
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Sentinel 2 Data");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Sentinel 2 Data (*.xml)",
                "*.xml"));
        this.sentinelTrainingFile = fc.showOpenDialog(osmTrainingPane.getScene().getWindow());
    }

    /**
     * Training shapefile
     */
    private File trainingShapefile = null;

    /**
     * Select OpenStreetMap Shapefile for model training
     * @param actionEvent open OSM training data action event
     */
    public void openOSMTrainingData(ActionEvent actionEvent) {
        //Select file
        FileChooser fc = new FileChooser();
        fc.setTitle("Open OSM Shapefile");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("OSM Shapefile (*.shp)",
                        "*.shp"));
        this.trainingShapefile = fc.showOpenDialog(osmTrainingPane.getScene().getWindow());
    }
}
