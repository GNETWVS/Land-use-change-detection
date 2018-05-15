package landUseChangeDetection.forms;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressForm {
        private final Stage dialogStage;
        private final ProgressBar pb = new ProgressBar();
        private final Label status = new Label();

        public ProgressForm() {
            dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setResizable(false);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Loading...");
            dialogStage.setWidth(400);
            dialogStage.setHeight(150);
            pb.setProgress(-1F);
            pb.setPrefWidth(380);
            final VBox hb = new VBox();
            hb.setSpacing(5);
            hb.setAlignment(Pos.CENTER);
            hb.getChildren().addAll(pb, status);

            Scene scene = new Scene(hb, 600, 400);
            dialogStage.setScene(scene);
        }

        public void activateProgressBar(final Task<?> task)  {
            pb.progressProperty().bind(task.progressProperty());
            status.textProperty().bind(task.messageProperty());
            dialogStage.show();
        }

        public Stage getDialogStage() {
            return dialogStage;
        }
}
