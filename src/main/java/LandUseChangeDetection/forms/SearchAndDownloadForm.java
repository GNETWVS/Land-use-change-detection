package LandUseChangeDetection.forms;

import LandUseChangeDetection.Utils;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.converter.NumberStringConverter;
import netscape.javascript.JSObject;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class SearchAndDownloadForm {

    /**
     * ESA open hub portal url
     */
    private static final String ESA_OPEN_HUB_PORTAL_URL = "https://scihub.copernicus.eu";

    /**
     * ESA Open Search base
     */
    private static final String OPEN_SEARCH_QUERY_BASE = "https://scihub.copernicus.eu/apihub/search?q=";

    /**
     * ESA open hub api url
     */
    private static final String esaOpenHubURL = "https://scihub.copernicus.eu/apihub/odata/v1/";

    public PasswordField passwordTextField;
    public TextField loginTextField;

    /**
     * OData consumer builder
     */
    private static ODataConsumer.Builder consumerBuilder = ODataConsumers.newBuilder(esaOpenHubURL);
    public DatePicker sensingStartDate;
    public DatePicker sensingFinishDate;
    public TextField maxCloudPercentage;
    public WebView webMap;

    /**
     * Open search client
     */
    private AbderaClient abderaClient;

    /**
     * OData consumer
     */
    private ODataConsumer consumer;

    /**
     * Geometry JS Leaflet string
     */
    private String geometryJS;

    /**
     * JS connector
     */
    public class DownloadAndSearchApplication {
        public void callFromJavascript(String geom) {
            geometryJS = geom;
        }
    }

    /**
     * Download form initialization
     */
    @FXML
    void initialize(){
        maxCloudPercentage.setTextFormatter(new TextFormatter<>(new NumberStringConverter()));
        WebEngine webEngine = webMap.getEngine();
        File mapIndexFile = new File("src/resources/SaDWebForm/index.html");
        webEngine.load("file:" + mapIndexFile.getAbsolutePath());
        webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("app", new DownloadAndSearchApplication());
            }
        });
    }

    /**
     * Check for login and password filling
     * @return empty or not
     */
    private boolean checkLoginAndPassword() {
        return loginTextField.getText().length() != 0 && passwordTextField.getText().length() != 0;
    }

    /**
     * Login acton handler
     * @param actionEvent login action event
     */
    public void loginHandler(ActionEvent actionEvent) throws URISyntaxException {
        this.loginTextField.setDisable(true);
        this.passwordTextField.setDisable(true);
        if (!checkLoginAndPassword()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Empty login or password");
            alert.setHeaderText("Error, empty login or password fields");
            alert.setContentText("Please, fill empty field or sing up in https://scihub.copernicus.eu/");
            alert.showAndWait();
            this.loginTextField.setDisable(false);
            this.passwordTextField.setDisable(false);

            return;
        }
        String login = loginTextField.getText();
        String password = passwordTextField.getText();
        // Create OpenSearchConsumer
        Abdera abdera = new Abdera();
        this.abderaClient = new AbderaClient(abdera);
        this.abderaClient.addCredentials(
                ESA_OPEN_HUB_PORTAL_URL,
                AuthScope.ANY_REALM,
                AuthScope.ANY_SCHEME,
                new UsernamePasswordCredentials(login, password)
        );
        // Create OData consumer
        consumerBuilder.setClientBehaviors(new BasicAuthenticationBehavior(login, password));
        this.consumer = consumerBuilder.build();
    }


    public void searchDataHandler(ActionEvent actionEvent) {
        Instant sensingStartDate = null;
        Instant sensingFinishDate = null;
        if (!this.sensingStartDate.getEditor().getText().isEmpty()) {
            LocalDate startDate = this.sensingStartDate.getValue();
            sensingStartDate = Instant.from(startDate.atStartOfDay(ZoneId.systemDefault()));
        }
        if (!this.sensingFinishDate.getEditor().getText().isEmpty()) {
            LocalDate finishDate = this.sensingFinishDate.getValue();
            sensingFinishDate = Instant.from(finishDate.atStartOfDay(ZoneId.systemDefault()));
        }
        if (this.maxCloudPercentage.getText().isEmpty()) {
            Utils.showErrorMessage("Max cloud percentage error",
                    "Max cloud percentage should be integer number between 0 and 100",
                    "");
            return;
        }
        int maxCloudsPercentage = Integer.parseInt(maxCloudPercentage.getText());
        if (maxCloudsPercentage < 0 || maxCloudsPercentage > 100) {
            Utils.showErrorMessage("Max cloud percentage error",
                    "Max cloud percentage should be integer number between 0 and 100",
                    "");
            return;
        }
        // TODO: Сравнить даты и на обе даты
        // Create query url
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("platformname:Sentinel-2");
        // Set up periods
        if (sensingStartDate != null && sensingFinishDate != null) {
            queryBuilder.append("&")
                    .append("endposition:%5B")
                    .append(sensingStartDate.toString())
                    .append("%20TO%20")
                    .append(sensingFinishDate.toString())
                    .append("%5D");
        }
        // Coverage intersection
        if (this.geometryJS != null) {
            queryBuilder.append("&footprint:\"Intersects(")
                    .append(geometryJS)
                    .append(")\"");
        }
        // Set up clouds percentage
        if (maxCloudsPercentage != 100) {
            queryBuilder.append("&cloudcoverpercentage:%5B0%20TO%20").append(maxCloudsPercentage).append("%5D");
        }
        // Create open search query
        ClientResponse response = this.abderaClient.get(OPEN_SEARCH_QUERY_BASE + queryBuilder.toString());
        List<Entry> entries = null;
        if (response.getType() == Response.ResponseType.SUCCESS) {
            Document<Feed> doc = response.getDocument();
            Feed feed = doc.getRoot();
            entries = feed.getEntries();
        } else {
            Utils.showErrorMessage("Error", "Open Search error", response.getType().toString());
            Alert alert = new Alert(Alert.AlertType.ERROR);
        }

//        for (OEntity entity : consumer.getEntities("Products")
//                .filter("startswith(Name,'S2') and year(IngestionDate) eq 2017")
//                .expand("Nodes")
//                .execute()) {
//            entity
//            System.out.println(entity.getProperties());
//        }
    }
}