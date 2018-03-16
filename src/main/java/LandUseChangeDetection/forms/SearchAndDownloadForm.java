package LandUseChangeDetection.forms;

import javafx.fxml.FXML;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.core.http.BasicAuthHttpClientFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;

public class SearchAndDownloadForm {

    private static final String esaOpenHubURL = "https://scihub.copernicus.eu/dhus/search";

    private HttpSolrClient solr;



    private ODataClient oDataClient;

    @FXML
    void initialize(){

        // TODO: Solr with
        // TODO: Solr result to oData
        this.oDataClient = ODataClientFactory.getClient();
        this.oDataClient.getConfiguration().setHttpClientFactory(new BasicAuthHttpClientFactory("[username]", "[password]"));



        this.solr = new HttpSolrClient.Builder(esaOpenHubURL).build();
        solr.setParser(new XMLResponseParser());

        // Query example
        SolrQuery query = new SolrQuery();
        query.setQuery("platformname:Sentinel-2");
        query.setFields("*");
        try {
            QueryResponse res = solr.query(query);
            SolrDocumentList list = res.getResults();
            for (SolrDocument document : list) {
                System.out.println(document);
            }
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
