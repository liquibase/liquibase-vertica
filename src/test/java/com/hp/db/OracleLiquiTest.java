package com.hp.db;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.change.ChangeFactory;
import liquibase.change.core.CreateTableChange;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by vesterma on 05/10/2014.
 */
public class OracleLiquiTest {

    public static void main(String[] args) throws Exception {
        Properties myProp = new Properties();

        myProp.put("user", "jony");
        myProp.put("password", "jony");
//        myProp.put("user", "maas_admin");
//        myProp.put("password", "maas_admin_123");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@//192.168.33.108:1521/orcl",
                    myProp);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        OracleDatabase od = new OracleDatabase();

        Scope.getCurrentScope().getSingleton(ChangeFactory.class).unregister("createTable");
        Scope.getCurrentScope().getSingleton(ChangeFactory.class).register(new CreateTableChange());

        DatabaseConnection dc = new JdbcConnection(conn);
        od.setConnection(dc);

        Liquibase liquibase = null;
        try {
            URL url = LiquiManager.class.getClassLoader().getResource("db_change2.xml");
//            URL url = LiquiManager.class.getClassLoader().getResource("EUM/liquibaseChangeSet.xml");
            // this code translates the project relative path to an absolute one.
            File f;
            try {
                f = new File(url.toURI());
            } catch (URISyntaxException e) {
                f = new File(url.getPath());
            }
            String changelog = f.getAbsolutePath();

            liquibase = new Liquibase(changelog, new DirectoryResourceAccessor(new File(".")), dc);
//            liquibase = new Liquibase("C:\\Users\\vesterma\\Documents\\Projects\\liquibase\\target\\classes\\com.hp.db\\db_change1.xml", new FileSystemResourceAccessor(),dc);

//            liquibase = new Liquibase("C:\\Temp\\test.xml", new FileSystemResourceAccessor(),dc);
//            liquibase.rollback(2,"");
            liquibase.update(2, "");
//            liquibase.changeLogSync("");
//            liquibase.generateDocumentation("c:\\temp");
//            String defaultCatalogName = "public";
            String defaultCatalogName = "bla";

//            String defaultSchemaName = "public";
  /*          String defaultSchemaName = "bla";
            String changeLogFile = "c:\\temp\\test.xml";
            String changeSetAuthor = "jony";
            String diffTypes = null;
            String changeSetContext = null;
            String dataOutputDirectory = "c:\\temp";

            boolean includeCatalog = false; //Boolean.parseBoolean(getCommandParam("includeCatalog", "false"));
            boolean includeSchema = false; //Boolean.parseBoolean(getCommandParam("includeSchema", "false"));
            boolean includeTablespace = false; //Boolean.parseBoolean(getCommandParam("includeTablespace", "false"));
            DiffOutputControl diffOutputControl = new DiffOutputControl(includeCatalog, includeSchema, includeTablespace);
*/
//            try {
//                DiffToChangeSetLog.doGenerateChangeLog(changeLogFile, verticaDatabase, defaultCatalogName, defaultSchemaName, StringUtil.trimToNull(diffTypes), StringUtil.trimToNull(changeSetAuthor), StringUtil.trimToNull(changeSetContext), StringUtil.trimToNull(dataOutputDirectory), diffOutputControl);
//                CommandLineUtils.doDiffToChangeLog(changeLogFile,verticaDatabase,verticaDatabase1,diffOutputControl,"tables,projections");
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (ParserConfigurationException e) {
//                e.printStackTrace();
//            }
        } catch (LiquibaseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
