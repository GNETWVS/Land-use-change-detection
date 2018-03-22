package LandUseChangeDetection.forms;

import javafx.fxml.FXML;

public class SearchAndDownloadForm {

    private static final String esaOpenHubURL = "https://scihub.copernicus.eu/apihub/odata/v1/";

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
}