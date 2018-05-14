package LandUseChangeDetection;

import LandUseChangeDetection.forms.ProgressForm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jdk.nashorn.api.scripting.JSObject;
import org.apache.commons.io.FileUtils;
import org.esa.s2tbx.dataio.VirtualPath;
import org.geotools.data.DataStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.hsqldb.lib.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {

    /**
     * Application form
     */
    public BorderPane appForm;
    public ComboBox beforeGranulesComboBox;
    public ComboBox afterGranulesComboBox;
    public WebView webMap;

    /**
     * Data resolutions
     */
    private final ObservableList<String> resolutions = FXCollections.observableArrayList( "60m", "20m");
    public ComboBox resolutionBox;
    public Label waterLabel;
    public Label buildLevel;
    public Label forestLabel;
    public Label waLabel;
    public Label wbLabel;
    public Label wfLabel;
    public Label awLabel;
    public Label abLabel;
    public Label afLabel;
    public Label bwLabel;
    public Label baLabel;
    public Label bfLabel;
    public Label faLabel;
    public Label fwLabel;
    public Label fbLabel;
    public Label agriLabel;
    public Label roiLabel;
    public TextArea wktTextArea;

    WebEngine webEngine;

    @FXML
    void initialize(){
        resolutionBox.setItems(resolutions);
        resolutionBox.setValue("60m");
        this.webEngine = webMap.getEngine();
        File mapIndexFile = new File("src/resources/AppWebForm/index.html");
        webEngine.load("file:" + mapIndexFile.getAbsolutePath());
        webEngine.setJavaScriptEnabled(true);
    }

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
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setScene(new Scene(root, 800, 600));
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/learnNextGISForm.fxml"));
        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Learn SVM by Open Street Map");
            stage.setScene(new Scene(root, 600, 400));
            stage.getIcons().add(new Image("/icon.png"));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private File beforeSentinelData;

    public void selectBeforeData(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fc.setSelectedExtensionFilter(filter);
        this.beforeSentinelData = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.beforeSentinelData == null) {
            Utils.showErrorMessage("Before Sentinel 2 file is not selected",
                    "Before Sentinel 2 file is not selected",
                    "Please, select Sentinel 2 data file");
        } else {
            try {
                List<VirtualPath> granules = SentinelData.checkAndGetGranules(this.beforeSentinelData);
                ObservableList<String> granulesList = FXCollections.observableArrayList(
                        granules.stream().map(VirtualPath::getFullPathString)
                                .collect(Collectors.toList()));
                beforeGranulesComboBox.setItems(granulesList);
                beforeGranulesComboBox.setValue(granulesList.get(0));
            } catch (Exception e) {
                Utils.showErrorMessage("Error",
                        e.getMessage(),
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private File afterSentinelData;

    public void selectAfterData(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fc.setSelectedExtensionFilter(filter);
        this.afterSentinelData = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.afterSentinelData == null) {
            Utils.showErrorMessage("After Sentinel 2 file is not selected",
                    "After Sentinel 2 file is not selected",
                    "Please, select Sentinel 2 data file");
        } else {
            try {
                List<VirtualPath> granules = SentinelData.checkAndGetGranules(this.afterSentinelData);
                ObservableList<String> granulesList = FXCollections.observableArrayList(
                        granules.stream().map(VirtualPath::getFullPathString)
                                .collect(Collectors.toList()));
                afterGranulesComboBox.setItems(granulesList);
                afterGranulesComboBox.setValue(granulesList.get(0));
            } catch (Exception e) {
                Utils.showErrorMessage("Error",
                        e.getMessage(),
                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private ChangeDetector lucd;

    public void detectChanges(ActionEvent actionEvent) {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        try {
            File beforeSentinelGranuleFile = new File(beforeGranulesComboBox.getValue().toString());
            File afterSentinelGranuleFile = new File(afterGranulesComboBox.getValue().toString());
            final Resolution resolution = resolutionBox.getValue().equals("60m") ? Resolution.R60m : Resolution.R20m;

            ProgressForm form = new ProgressForm();
            Task<ChangeDetector> task = new Task<ChangeDetector>() {
                @Override
                protected ChangeDetector call() throws Exception {
                    updateProgress(0, 1);
                    updateMessage("Opening and parsing of " + beforeSentinelGranuleFile.getName());
                    SentinelData firstSentinelData = new SentinelData(beforeSentinelGranuleFile, resolution, SentinelData.getType(beforeSentinelData));
                    updateProgress(0.1, 1);
                    updateMessage("Opening and parsing of " + afterSentinelGranuleFile.getName());
                    SentinelData secondSentinelData = new SentinelData(afterSentinelGranuleFile, resolution, SentinelData.getType(afterSentinelData));
                    updateProgress(0.2, 1);
                    updateMessage("Bands cropping...");
                    ChangeDetector detector;
                    if (roiFile == null) {
                        detector = new ChangeDetector(firstSentinelData, secondSentinelData);
                    } else {
                        DataStore store = Utils.openShapefile(roiFile);
                        if (store == null || store.getTypeNames() == null || store.getTypeNames().length == 0) {
                            throw new NullPointerException("ROI vector data store is null");
                        }
                        String waterTypeName = store.getTypeNames()[0];
                        detector = new ChangeDetector(firstSentinelData, secondSentinelData,
                                Utils.openShapefile(roiFile).getFeatureSource(waterTypeName).getFeatures());
                    }
                    updateProgress(0.3, 1);
                    updateMessage("Bands classification and clouds and snow removing...");
                    detector.certificate();
                    updateProgress(0.5, 1);
                    updateMessage("Land-use classification results checking and fixing...");
                    detector.checkAndFixPixels();
                    updateProgress(0.6, 1);
                    updateMessage("Land-use classification vector extraction...");
                    detector.extractPolygons();
                    updateProgress(0.7, 1);
                    updateMessage("Land-use change detection...");
                    detector.detectLandUseChanges();
                    updateProgress(0.8, 1);
                    updateMessage("Land-use change areas calculation...");
                    detector.calculateLUCDAreas();
                    updateProgress(0.9, 1);
                    return detector;
                }
            };
            form.activateProgressBar(task);
            task.setOnSucceeded(event -> {
                try {
                    this.lucd = task.getValue();
                    List<LandUseChangeDetectionResult> areas = lucd.getAreas();
                    for (LandUseChangeDetectionResult area : areas) {
                        switch (area.getBefore()) {
                            case 0: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        waterLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 1: {
                                        waLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 2: {
                                        wbLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 3: {
                                        wfLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 1: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        awLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 1: {
                                        agriLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 2: {
                                        abLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 3: {
                                        afLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 2: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        bwLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 1: {
                                        baLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                   }
                                    case 2: {
                                        buildLevel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 3: {
                                        bfLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                }
                                break;
                            }
                            case 3: {
                                switch (area.getAfter()) {
                                    case 0: {
                                        fwLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 1: {
                                        faLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 2: {
                                        fbLabel.textProperty().setValue(area.getArea() + " m²");
                                        break;
                                    }
                                    case 3: {
                                        forestLabel.textProperty().setValue(area.getArea() + " m²" );
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    Utils.writeGeoJSON(this.lucd.getChangeDetection(), "src/resources/AppWebForm/res/result.json");
                    this.wktTextArea.textProperty().setValue(this.lucd.getWKT());
                    this.webEngine.executeScript("showResult();");
                } catch (Exception e) {
                    Utils.showErrorMessage("Error",
                            e.getMessage(),
                            Arrays.toString(e.getStackTrace()));
                } finally {
                    form.getDialogStage().close();
                }
            });
            task.setOnFailed(e -> {
                Utils.showErrorMessage("Error",
                        task.getException().getMessage(),
                        Arrays.toString(task.getException().getStackTrace()));
                form.getDialogStage().close();
            });
            ((Stage)this.appForm.getScene().getWindow()).setOnHiding(event -> task.cancel());

            new Thread(task).start();

        } catch (Exception e) {
            Utils.showErrorMessage("Error",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
        }
    }

    private File roiFile;

    public void selectROI(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Shapefile (*.shp)", "*.shp");
        fc.setSelectedExtensionFilter(filter);
        this.roiFile = fc.showOpenDialog(appForm.getScene().getWindow());
        if (this.roiFile == null) {
            roiLabel.setText("");
        } else {
            roiLabel.setText(roiFile.getAbsolutePath());
        }
    }

    public void openLUCD(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.lucd)", "*.lucd");
        fc.setSelectedExtensionFilter(filter);
        File file = fc.showOpenDialog(appForm.getScene().getWindow());
        if (file == null || !file.exists()) {
            Utils.showErrorMessage("Error",
                    "Please select LUCD file",
                    "");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            CDSer lucd = (CDSer) ois.readObject();
            List<LandUseChangeDetectionResult> areas = lucd.resultList;
            for (LandUseChangeDetectionResult area : areas) {
                switch (area.getBefore()) {
                    case 0: {
                        switch (area.getAfter()) {
                            case 0: {
                                waterLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 1: {
                                waLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 2: {
                                wbLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 3: {
                                wfLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                        }
                        break;
                    }
                    case 1: {
                        switch (area.getAfter()) {
                            case 0: {
                                awLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 1: {
                                agriLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 2: {
                                abLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 3: {
                                afLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                        }
                        break;
                    }
                    case 2: {
                        switch (area.getAfter()) {
                            case 0: {
                                bwLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 1: {
                                baLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 2: {
                                buildLevel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 3: {
                                bfLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                        }
                        break;
                    }
                    case 3: {
                        switch (area.getAfter()) {
                            case 0: {
                                fwLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 1: {
                                faLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 2: {
                                fbLabel.textProperty().setValue(area.getArea() + " m²");
                                break;
                            }
                            case 3: {
                                forestLabel.textProperty().setValue(area.getArea() + " m²" );
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            PrintWriter writer = new PrintWriter(new File("src/resources/AppWebForm/res/result.json"));
            writer.write(lucd.json);
            writer.close();
            this.wktTextArea.textProperty().setValue(lucd.wkt);
            this.webEngine.executeScript("showResult();");
        } catch (Exception e) {
            Utils.showErrorMessage("Error",
                    "Cannot open LUCD file",
                    "");
        }
    }

    public void saveLUCD(ActionEvent actionEvent) {
        if (this.lucd == null) {
            Utils.showErrorMessage("Error",
                    "Please, make Land Use Change Detection Before",
                    "");
            return;
        }
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.lucd)", "*.lucd");
        fc.setSelectedExtensionFilter(filter);
        File file = fc.showSaveDialog(appForm.getScene().getWindow());
        if (file != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                String gson = "";
                FileInputStream inputStream = new FileInputStream("src/resources/AppWebForm/res/result.json");
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
                while (scanner.hasNextLine()) {
                    gson += scanner.nextLine();
                }
                scanner.close();
                inputStream.close();
                CDSer s = new CDSer(this.lucd.getAreas(), gson, this.wktTextArea.getText());
                oos.writeObject(s);
            } catch (IOException e) {
                e.printStackTrace();
                Utils.showErrorMessage("Error",
                        "Cannot write LUCD file",
                        "");
            }
        }
    }

    public void saveSHP(ActionEvent actionEvent) {
        if (this.lucd == null) {
            Utils.showErrorMessage("Error",
                    "Please, make Land Use Change Detection Before",
                    "");
            return;
        }
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("XML files (*.shp)", "*.shp");
        fc.setSelectedExtensionFilter(filter);
        File file = fc.showSaveDialog(appForm.getScene().getWindow());
        if (file != null) {
            try {
                Utils.writeShapefile(this.lucd.getChangeDetection(), file.getAbsolutePath());
            } catch (IOException e) {
                Utils.showErrorMessage("Error",
                        "Cannot write Shapefile",
                        "");
            }
        }
    }
}