package LandUseChangeDetection.data;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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


    public static FeatureCollection getLandUseChanges(SimpleFeatureCollection before, Date beforeDate,
                                                      SimpleFeatureCollection after, Date afterDate) throws SQLException {
        // Insert both collection to PostGIS
        insertCollection(before, beforeDate);
        insertCollection(after, afterDate);
        return  null;
    }

    /**
     * Insertion into PostGIS statement
     */
    private static final String INSERT_STATEMENT = "INSERT INTO landUses(sensingDate, geom, landUseClass) VALUES (?, st_geomfromtext(?), ?)";

    private static void insertCollection(SimpleFeatureCollection collection, Date date) throws SQLException {
        SimpleFeatureIterator it = collection.features();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            Geometry geometry = (Geometry)feature.getDefaultGeometry();
            int landUseClass = ((Double)feature.getAttribute("value")).intValue();
            PreparedStatement statement = connection.prepareStatement(INSERT_STATEMENT);
            statement.setDate(1, new java.sql.Date(date.getTime()));
            statement.setString(2, geometry.toText());
            statement.setInt(3, landUseClass);
            statement.execute();
        }
    }
}
