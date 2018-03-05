package LandUseChangeDetection.forms;

import LandUseChangeDetection.SentinelLevelUpdater;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

import java.io.File;

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
     * Sentinel 2 Level 1C file
     */
    private File sentinel1CLevelFile = null;

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
     * Level up process
     */
    private Process levelUpProcess = null;

    /**
     * Create Sentinel 2 Level 2A Data
     * @param actionEvent Creating action event
     */
    public void createL2ADataHandler(ActionEvent actionEvent) {
        try {
            create2ADataButton.setDisable(true);
            SentinelLevelUpdater updater = new SentinelLevelUpdater(this);
            this.levelUpProcess = updater.levelUp(this.sentinel1CLevelFile);
            cancelButton.setDisable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel level up
     * @param actionEvent cancel level up
     */
    public void cancelLevelUp(ActionEvent actionEvent) {
        levelUpProcess.destroy();
    }

    public void finishProcess() {
        cancelButton.setDisable(true);
        create2ADataButton.setDisable(false);
    }
}
