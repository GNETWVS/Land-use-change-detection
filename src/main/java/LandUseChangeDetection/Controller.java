package LandUseChangeDetection;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class Controller {

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
        StackPane layout = new StackPane();

        Scene nextGISScene = new Scene(layout, 230, 100);

        Stage learnFromNextGISStage = new Stage();
        learnFromNextGISStage.setTitle("Learn by NextGIS");
        learnFromNextGISStage.setScene(nextGISScene);

        learnFromNextGISStage.show();
    }
}
