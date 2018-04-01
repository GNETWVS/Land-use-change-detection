package LandUseChangeDetection.data;

import org.geotools.feature.FeatureCollection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


    public static FeatureCollection getLandUseChanges(FeatureCollection before, FeatureCollection after) {

        return  null;
    }
}
