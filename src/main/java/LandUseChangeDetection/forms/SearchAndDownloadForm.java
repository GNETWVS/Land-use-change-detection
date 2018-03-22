package LandUseChangeDetection.forms;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;

import java.util.Date;

public class SearchAndDownloadForm {

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

    /**
     * OData consumer
     */
    private ODataConsumer consumer;

    @FXML
    void initialize(){
//        CredentialsProvider provider = new BasicCredentialsProvider();
        String login = "artur7";
        String password = "9063228328a!";
//        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login, password);
//        provider.setCredentials(AuthScope.ANY, credentials);
//        HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
//        // TODO: Solr with Delete exmaple
//        HttpSolrClient solrClient = new HttpSolrClient.Builder(esaOpenHubURL)
//                .withHttpClient(httpClient)
//                .build();
//        solrClient.setParser(new Parser()); // TODO:Atom Parsing
//        SolrQuery query = new SolrQuery().setParam(CommonParams.QT, "/search");
//        query.setStart(0);
//        query.setRows(10);
//        query.setQuery("platformname:Sentinel-2");
//        try {
//            QueryResponse response = solrClient.query(query);
//            System.out.println(response.getResults());
//        } catch (SolrServerException | IOException e) {
//            e.printStackTrace();
//        }
        // TODO: Solr result to oData
        // TODO: OData
//        ODataClient client = ODataClientFactory.getClient();
//        client.getConfiguration().setHttpClientFactory(
//                new BasicAuthHttpClientFactory(login, password)
//        );
//
//
//        EdmMetadataRequest request1 = client.getRetrieveRequestFactory().getMetadataRequest();
//        System.out.println(request1.getURI());
//        System.out.println(request1.getPrefer());
//        request1.setAccept("application/xml");
//
//        ODataRetrieveResponse<Edm> response1 = request1.execute();
//        Edm edm = response1.getBody();
//        List<FullQualifiedName> ctFqns = new ArrayList<FullQualifiedName>();
//        List<FullQualifiedName> etFqns = new ArrayList<FullQualifiedName>();
//        System.out.println("\n----- Inspect each property and its type of the first entity: " + etFqns.get(0) + "----");
//        for (EdmSchema schema : edm.getSchemas()) {
//            for (EdmComplexType complexType : schema.getComplexTypes()) {
//                ctFqns.add(complexType.getFullQualifiedName());
//            }
//            for (EdmEntityType entityType : schema.getEntityTypes()) {
//                etFqns.add(entityType.getFullQualifiedName());
//            }
//        }
//        System.out.println("Found ComplexTypes" + ctFqns);
//        System.out.println("Found EntityTypes" + etFqns);
//
//        System.out.println("\n----- Inspect each property and its type of the first entity: " + etFqns.get(0) + "----");
//        EdmEntityType etype = edm.getEntityType(etFqns.get(0));
//        for (String propertyName : etype.getPropertyNames()) {
//            EdmProperty property = etype.getStructuralProperty(propertyName);
//            FullQualifiedName typeName = property.getType().getFullQualifiedName();
//            System.out.println("property '" + propertyName + "' " + typeName);
//        }
//
//
//        URI uri = client.newURIBuilder("https://scihub.copernicus.eu/apihub/odata/v1/Products?$filter=startswith(Name,%27S1%27)&&$format=atom").build();
//        System.out.println("URI = " + uri);
//        ODataEntitySetIteratorRequest<ClientEntitySet, ClientEntity> request =
//                client.getRetrieveRequestFactory().getEntitySetIteratorRequest(uri);
//        request.setAccept("application/atom+xml");
//        ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response = request.execute();
//        ClientEntitySetIterator<ClientEntitySet, ClientEntity> iterator = response.getBody();
//        while (iterator.hasNext()) {
//            ClientEntity ce = iterator.next();
//            System.out.println("Entry:" + ce.getProperties());
//        }
//        SearchFactory f = new SearchFactoryImpl().
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
    public void loginHandler(ActionEvent actionEvent) {
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
        // Create consumer
        String login = loginTextField.getText();
        String password = passwordTextField.getText();
        consumerBuilder.setClientBehaviors(new BasicAuthenticationBehavior(login, password));
        this.consumer = consumerBuilder.build();
    }


    public void searchDataHandler(ActionEvent actionEvent) {
        System.out.println(sensingStartDate.getEditor().getText());
        for (OEntity entity : consumer.getEntities("Products")
                .filter("startswith(Name,'S2') and year(IngestionDate) eq 2017")
                .expand("Nodes")
                .execute()) {
            System.out.println(entity.getProperties());
        }
    }
}