package LandUseChangeDetection.forms;

import LandUseChangeDetection.Classification;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class TrainNextGisForm {

    /**
     * Learn SVM by NextGIS data anchor pane
     */
    public AnchorPane learnNextGISForm;

    /**
     * Training sentinel file
     */
    private File sentinel2ALevelFile = null;

    /**
     * Selection of Sentinel training files
     * @param actionEvent Selection action event
     */
    public void selectSentinelTrainingData(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.sentinel2ALevelFile = dc.showDialog(learnNextGISForm.getScene().getWindow());
        if (this.sentinel2ALevelFile == null) {
            //pathLabel.setText("Data not selected");
            //create2ADataButton.setDisable(true);
            //cancelButton.setDisable(true);
        } else {
            //pathLabel.setText(this.sentinel1CLevelFile.getAbsolutePath());
            //create2ADataButton.setDisable(false);
        }
    }

    private File trainingShapefile = null;

    public void selectTrainingVectorFile(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.trainingShapefile = dc.showDialog(learnNextGISForm.getScene().getWindow());
        if (this.trainingShapefile == null) {

        } else {

        }
    }

    public void trainSVMModel(ActionEvent actionEvent) {
        if (this.sentinel2ALevelFile == null || this.trainingShapefile == null) {
            return;
        }
        Classification svm = Classification.getInstance();
        try {
            svm.trainByNextGISData(this.trainingShapefile, this.sentinel2ALevelFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
