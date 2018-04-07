package LandUseChangeDetection.data;

import LandUseChangeDetection.Utils;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.WKTReader;
import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.postgis.PGgeometry;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

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


    public static SimpleFeatureCollection getLandUseChanges(SimpleFeatureCollection before, Date beforeDate,
                                                      SimpleFeatureCollection after, Date afterDate) throws Exception {
        // Insert both collection to PostGIS
        insertCollection(before, beforeDate);
        insertCollection(after, afterDate);
        if (before.getSchema().getCoordinateReferenceSystem() != after.getSchema().getCoordinateReferenceSystem()) {
            Utils.transformToCRS(after, before.getSchema().getCoordinateReferenceSystem());
        }
        // Get changes
        return getLandUseChanges(beforeDate, afterDate, before.getSchema().getCoordinateReferenceSystem());
    }

    /**
     * Insert clssificated collection into PostGIS
     * @param collection classificated collection
     * @param date sensing date
     */
    private static void insertCollection(SimpleFeatureCollection collection, Date date) throws IOException, FactoryException {
        // Set time of date to 0
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
        );
        date = calendar.getTime();

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
        SimpleFeatureType featureType = store.getSchema();
        System.out.println(collection.getSchema().getCoordinateReferenceSystem());
        List<SimpleFeature> list = new ArrayList<>();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        SimpleFeatureIterator it = collection.features();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            featureBuilder.addAll(feature.getAttributes());
            featureBuilder.add(new java.sql.Date(date.getTime()));
            SimpleFeature datedFeature = featureBuilder.buildFeature(null);
            list.add(datedFeature);
        }
        System.out.println(store.getSchema().getCoordinateReferenceSystem());
        SimpleFeatureCollection dateFeatureCollection = new ListFeatureCollection(featureType, list);
        CoordinateReferenceSystem crs = store.getSchema().getCoordinateReferenceSystem();
        dateFeatureCollection = Utils.transformToCRS(dateFeatureCollection, crs);
        it = dateFeatureCollection.features();
        while (it.hasNext()) {
            System.out.println(it.next().getDefaultGeometry());
        }
        Transaction tx = new DefaultTransaction("Add landuses");
        store.setTransaction(tx);
        try {
            store.addFeatures(dateFeatureCollection);
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
     * Get changes query
     */
    private static final String CHANGE_DETECTION_QUERY =
            "SELECT st_intersection(before.geom, after.geom), " +
                    "before.landuseclass AS beforeCLass, after.landuseclass AS afterClass " +
                    "FROM (SELECT * FROM landuses WHERE sensingdate = ?) AS before " +
                    "INNER JOIN (SELECT * FROM landuses WHERE sensingdate = ?) AS after " +
                    "ON before.sensingdate != after.sensingdate AND st_intersects(before.geom, after.geom)";


//    private static final String CHANGE_DETECTION_QUERY;
//    static {
//        StringBuilder queryBuilder = new StringBuilder();
//        queryBuilder.append("SELECT row_to_json(fc) ");
//        queryBuilder.append("FROM (SELECT 'FeatureCollection' AS type, array_to_json(array_agg(f)) AS features ");
//        queryBuilder.append("FROM (SELECT 'Feature' AS type, ");
//        queryBuilder.append("st_asGeoJSON(st_intersection(before.geom, after.geom))::json AS geometry, ");
//        queryBuilder.append("row_to_json((before.landuseclass, after.landuseclass)) AS properties ");
//        queryBuilder.append("FROM (SELECT * FROM landuses WHERE sensingdate = ?) AS before ");
//        queryBuilder.append("INNER JOIN (SELECT * FROM landuses WHERE sensingdate = ?) AS after ");
//        queryBuilder.append("ON before.sensingdate != after.sensingdate AND st_intersects(before.geom, after.geom) AND before.landuseclass = after.landuseclass) AS f) AS fc");
//        CHANGE_DETECTION_QUERY = queryBuilder.toString();
//    }

    /**
     * Get land use changes
     * @param firstDate before sensing date
     * @param afterDate after sensing date
     * @return classification detection
     * @throws SQLException if cannot read from PostGIS
     */
    private static SimpleFeatureCollection getLandUseChanges(Date firstDate, Date afterDate, CoordinateReferenceSystem crs) throws Exception {
        PreparedStatement statement = connection.prepareStatement(CHANGE_DETECTION_QUERY);
        statement.setDate(1, new java.sql.Date(firstDate.getTime()));
        statement.setDate(2,  new java.sql.Date(afterDate.getTime()));
        ResultSet resultSet = statement.executeQuery();
//        if (!resultSet.next()) {
//            throw new Exception("Error, change detection is null");
//        }
//        String lucdGeoJson = resultSet.getString(1);
//        GeometryJSON gjson = new GeometryJSON(15);
//        FeatureJSON fjson = new FeatureJSON(gjson);
//        FeatureCollection fc = fjson.readFeatureCollection(lucdGeoJson);
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("LUCD");
        typeBuilder.setCRS(crs);
        typeBuilder.add("geom", MultiPolygon.class);
        typeBuilder.add("before", Integer.class);
        typeBuilder.add("after", Integer.class);
        final SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, null);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        while (resultSet.next()) {
            PGgeometry geom = (PGgeometry)resultSet.getObject(1);
            int beforeClass = resultSet.getInt(2);
            int afterCLass = resultSet.getInt(3);
            System.out.println(geom.getGeometry());
            featureBuilder.add(geom.getGeometry());
            featureBuilder.add(beforeClass);
            featureBuilder.add(afterCLass);
            SimpleFeature feature = featureBuilder.buildFeature(null);
            featureCollection.add(feature);
        }
        return featureCollection;
        //return DataUtilities.simple(fc);
        //return null;
    }
}