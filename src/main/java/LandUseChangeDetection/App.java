package LandUseChangeDetection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Hello world!
 *
 */
public class App extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/app.fxml"));
        primaryStage.setTitle("Land-Use Change Detector");
        primaryStage.setScene(new Scene(root, 800, 700));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(700);
        primaryStage.getIcons().add(new Image("/icon.png"));
        primaryStage.show();
    }

    public static void main( String[] args )
    {
        launch(args);
    }
}