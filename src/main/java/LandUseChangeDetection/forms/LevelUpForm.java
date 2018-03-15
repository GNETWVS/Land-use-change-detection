package LandUseChangeDetection.forms;

import LandUseChangeDetection.SentinelLevelUpdater;
import LandUseChangeDetection.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LevelUpForm {

    /**
     * Level up form's anchor pane
     */
    public AnchorPane levelUpForm;

    /**
     * Sentinel 2 Data path label
     */
    public Label pathLabel;

    /**
     * Current task label
     */
    public Label taskLabel;

    /**
     * Sentinel 2 Level up progress bar
     */
    public ProgressBar progressBar;

    /**
     * Start level up button
     */
    public Button create2ADataButton;

    /**
     * Interrupt creation button
     */
    public Button cancelButton;

    /**
     * Select file button
     */
    public Button openSentinel1;

    /**
     * Data resolutions
     */
    private final ObservableList<String> resolutions = FXCollections.observableArrayList("All", "10m", "20m", "60m");

    /**
     * Resolution choice box
     */
    public ChoiceBox resChoiceBox;

    /**
     * Sentinel 2 Level 1C file
     */
    private File sentinel1CLevelFile = null;

    @FXML
    void initialize() {
        resChoiceBox.setItems(resolutions);
        resChoiceBox.setValue("All");
    }

    /**
     * Open Sentinel 2 Level 1C file
     * @param actionEvent Opening action event
     */
    public void openSentinel1CLevelData(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        this.sentinel1CLevelFile = dc.showDialog(levelUpForm.getScene().getWindow());
        if (this.sentinel1CLevelFile == null) {
            pathLabel.setText("Data not selected");
            create2ADataButton.setDisable(true);
            cancelButton.setDisable(true);
        } else {
            pathLabel.setText(this.sentinel1CLevelFile.getAbsolutePath());
            create2ADataButton.setDisable(false);
        }
    }

    /**
     * Sentinel Level updater
     */
    private SentinelLevelUpdater updater;

    /**
     * Create Sentinel 2 Level 2A Data
     * @param actionEvent Creating action event
     */
    public void createL2ADataHandler(ActionEvent actionEvent) {
        if (this.sentinel1CLevelFile == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Sentinel 2 Data Not Selected");
            alert.setContentText("Please, select Sentinel 2 Level 1C File");
            alert.showAndWait();
            return;
        }
        if (Utils.isLevel2A(this.sentinel1CLevelFile)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText("Converting is not needed");
            alert.setContentText("Sentinel 2 Data already on Level 2A");
            alert.showAndWait();
            return;
        }
        openSentinel1.setDisable(true);
        create2ADataButton.setDisable(true);
        cancelButton.setDisable(false);
        resChoiceBox.setDisable(true);
        String temp = (String)resChoiceBox.getValue();
        if (temp.equals("All")){
            temp = "";
        } else {
            temp = "--resolution=" + temp.substring(0, temp.length() - 1);
        }
        final String param = temp;
        this.updater = new SentinelLevelUpdater();
        // Update Task
        Task updateTask = new Task() {
            @Override
            protected Object call() {
                try {
                    updater.levelUp(sentinel1CLevelFile, param);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(updater.getProcess().getInputStream(), "cp866"));
                    String line;
                    Pattern pattern = Pattern.compile("Progress\\[%]: (\\d+\\.\\d+) : (.*)");
                    while ((line = reader.readLine()) != null) {
                        // Checking for cancel
                        if (this.isCancelled()) {
                            Runtime rt = Runtime.getRuntime();
                            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                                for (int i = 0; i < 3; ++i) {
                                    rt.exec("taskkill /F /IM python.exe");
                                }
                            } else {
                                rt.exec("kill -9 python.exe");
                            }
                            updater.getProcess().destroy();
                            break;
                        }
                        Matcher m = pattern.matcher(line);
                        if (m.find()) {
                            updateMessage(m.group(2));
                            updateProgress(Double.parseDouble(m.group(1)), 100.0);
                        } else {
                            updateMessage(line);
                        }
                    }
                    updateMessage("Canceled");
                    updateProgress(0.0, 100.0);
                    openSentinel1.setDisable(false);
                    cancelButton.setDisable(true);
                    create2ADataButton.setDisable(false);
                    resChoiceBox.setDisable(false);
                } catch (Exception e) {
                    SentinelLevelUpdater.SEMAPHORE.release();
                    e.printStackTrace();
                } finally {
                    SentinelLevelUpdater.SEMAPHORE.release();
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(updateTask.progressProperty());
        taskLabel.textProperty().bind(updateTask.messageProperty());

        // Task cancel
        cancelButton.setOnMouseClicked(event -> updateTask.cancel());
        // Stage exit
        ((Stage)levelUpForm.getScene().getWindow()).setOnHiding(event -> updateTask.cancel());

        new Thread(updateTask).start();
    }
}
