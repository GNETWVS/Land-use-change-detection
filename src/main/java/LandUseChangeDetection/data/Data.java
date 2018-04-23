package LandUseChangeDetection.data;

import LandUseChangeDetection.LandUseChangeDetectionResult;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Data {

    /**
     * Sql connection
     */
    private static Connection connection;

    static {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5433/LUCD",
                    "postgres", "admin");
            // Add geometry type to the connection
            ((PGConnection)connection).addDataType("geometry", (Class<? extends PGobject>) Class.forName("org.postgis.PGgeometry"));
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


    public static List<LandUseChangeDetectionResult> getSquares(SimpleFeatureCollection collection) throws Exception {
        // Insert both collection to PostGIS
        insertCollection(collection);
        List<LandUseChangeDetectionResult> results = getLandUseChanges();
        clearDB();
        return results;
    }

    /**
     * Insert clssificated collection into PostGIS
     * @param collection classificated collection
     */
    private static void insertCollection(SimpleFeatureCollection collection) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("dbtype", "postgis");
        params.put("host", "localhost");
        params.put("port", 5433);
        params.put("schema", "public");
        params.put("database", "LUCD");
        params.put("user", "postgres");
        params.put("passwd", "admin");
        DataStore pgStore = DataStoreFinder.getDataStore(params);
        SimpleFeatureStore store = (SimpleFeatureStore) pgStore.getFeatureSource("landuses");

        Transaction tx = new DefaultTransaction("Add");
        store.setTransaction(tx);
        try {
            store.addFeatures(collection);
            tx.commit();
        } catch (Exception ex) {
            tx.rollback();
            throw ex;
        } finally {
            tx.close();
        }
        pgStore.dispose();
    }

    /**
     * Land use change areas result
     */
    private static final String CHANGE_SQUARES_QUERY;
    static {
        CHANGE_SQUARES_QUERY = "SELECT sum(st_area(the_geom)), before, after " +
                "FROM landuses " +
                "GROUP BY before, after ";
    }

    /**
     * Get land use change areas
     * @return list of change results
     */
    private static List<LandUseChangeDetectionResult> getLandUseChanges() throws Exception {
        PreparedStatement statement = connection.prepareStatement(CHANGE_SQUARES_QUERY);
        ResultSet resultSet = statement.executeQuery();
        List<LandUseChangeDetectionResult> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add(new LandUseChangeDetectionResult(
                    resultSet.getDouble(1),
                    resultSet.getInt(2),
                    resultSet.getInt(3)));
        }
        return results;
    }

    private static void clearDB() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("TRUNCATE landuses");
        statement.execute();
    }
}