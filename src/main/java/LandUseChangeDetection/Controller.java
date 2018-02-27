package LandUseChangeDetection;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Controller {

    public void openNextGisLearningForm(ActionEvent actionEvent) {
        StackPane layout = new StackPane();

        Scene nextGISScene = new Scene(layout, 230, 100);

        Stage learnFromNextGISStage = new Stage();
        learnFromNextGISStage.setTitle("Learn by NextGIS");
        learnFromNextGISStage.setScene(nextGISScene);

        learnFromNextGISStage.show();
    }
}
