package liquibase.ext.vertica;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 01/12/13
 * Time: 14:31
 * To change this template use File | Settings | File Templates.
 */
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/*
 * Class used by tests to set up connection and clean database.
 */
public class BaseTestCase {

    private static String url;
    private static Driver driver;
    private static Properties info;
    protected static Connection connection;
    protected static JdbcConnection jdbcConnection;
    protected static Liquibase liquiBase;
    protected static String changeLogFile;

    public static void connectToDB() throws Exception {
        if (connection == null) {
            info = new Properties();
            info.load(new FileInputStream("src/test/resources/liquibase.properties"));

            url = info.getProperty("url");
            driver = (Driver) Class.forName(DatabaseFactory.getInstance().findDefaultDriver(url), true,
                    Thread.currentThread().getContextClassLoader()).newInstance();

            connection = driver.connect(url, info);

            if (connection == null) {
                throw new DatabaseException("Connection could not be created to " + url + " with driver "
                        + driver.getClass().getName() + ".  Possibly the wrong driver for the given database URL");
            }

            jdbcConnection = new JdbcConnection(connection);
        }
    }

    public static void cleanDB() throws Exception {
        liquiBase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), jdbcConnection);
        liquiBase.dropAll();
    }
}