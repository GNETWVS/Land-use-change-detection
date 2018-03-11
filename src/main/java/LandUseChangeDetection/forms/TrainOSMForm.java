package LandUseChangeDetection.forms;

import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class TrainOSMForm {

    /**
     * OSM Training anchor pane
     */
    public AnchorPane osmTrainingPane;

    /**
     * Training sentinel data
     */
    private File sentinel2ALevelFile = null;

    /**
     *
     * @param actionEvent
     */
    public void selectSentinelTrainingData(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.sentinel2ALevelFile = dc.showDialog(osmTrainingPane.getScene().getWindow());
        if (this.sentinel2ALevelFile == null) {
            //pathLabel.setText("Data not selected");
            //create2ADataButton.setDisable(true);
            //cancelButton.setDisable(true);
        } else {
            //pathLabel.setText(this.sentinel1CLevelFile.getAbsolutePath());
            //create2ADataButton.setDisable(false);
        }
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
