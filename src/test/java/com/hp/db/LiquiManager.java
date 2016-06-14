package com.hp.db;

import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorFactory;
import liquibase.diff.output.changelog.core.MissingColumnChangeGenerator;
import liquibase.exception.LiquibaseException;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.diff.output.changelog.DiffToChangeSetLog;
import liquibase.ext.vertica.snapshot.ColumnVerticaSnapshotGenerator;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.snapshot.jvm.ForeignKeySnapshotGenerator;
import liquibase.snapshot.jvm.UniqueConstraintSnapshotGenerator;
import liquibase.util.StringUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 10/11/13
 * Time: 21:47
 * To change this template use File | Settings | File Templates.
 */
public class LiquiManager {
    public static void main(String[] args){

        VerticaDatabase verticaDatabase = new VerticaDatabase();
//        verticaDatabase.setDefaultSchemaName("maas");
        verticaDatabase.setDefaultSchemaName("public");
//        VerticaDatabase verticaDatabase1 = new VerticaDatabase();

        liquibase.database.DatabaseFactory.getInstance().clearRegistry();
        liquibase.database.DatabaseFactory.getInstance().register(verticaDatabase);
        liquibase.snapshot.SnapshotGeneratorFactory.getInstance().unregister(UniqueConstraintSnapshotGenerator.class);
        liquibase.snapshot.SnapshotGeneratorFactory.getInstance().unregister(ForeignKeySnapshotGenerator.class);
        liquibase.snapshot.SnapshotGeneratorFactory.getInstance().register(new ColumnVerticaSnapshotGenerator());

        ChangeGeneratorFactory.getInstance().unregister(MissingColumnChangeGenerator.class);




        Properties myProp = new Properties();

        myProp.put("user", "maas_admin");
        myProp.put("password", "maas_admin_123");
//        myProp.put("user", "maas_admin");
//        myProp.put("password", "maas_admin_123");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:vertica://localhost:5433/CIDB2",
                    myProp);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }



        DatabaseConnection dc = new JdbcConnection(conn);
        verticaDatabase.setConnection(dc);

        Liquibase liquibase = null;
        try {
//            URL url = LiquiManager.class.getClassLoader().getResource("vertica_mohamed/dbchangelog.xml");
            URL url = LiquiManager.class.getClassLoader().getResource("EUM/verticaMetricTableChangeSet.xml");
            // this code translates the project relative path to an absolute one.
            File f;
            try {
                f = new File(url.toURI());
            } catch(URISyntaxException e) {
                f = new File(url.getPath());
            }
            String changelog = f.getAbsolutePath();

            liquibase = new Liquibase(changelog,new FileSystemResourceAccessor(),verticaDatabase);
//            liquibase = new Liquibase("C:\\Users\\vesterma\\Documents\\Projects\\liquibase\\target\\classes\\com.hp.db\\db_change1.xml", new FileSystemResourceAccessor(),dc);

//            liquibase = new Liquibase("C:\\Temp\\test.xml", new FileSystemResourceAccessor(),dc);
//            liquibase.rollback(2,"");
            liquibase.update(11,"");
//            liquibase.changeLogSync("");
//            liquibase.generateDocumentation("c:\\temp");
            String defaultCatalogName = "public";
//            String defaultCatalogName = "bla";

            String defaultSchemaName = "maas";
//            String defaultSchemaName = "bla";
            String changeLogFile = "c:\\temp\\test.xml";
            String changeSetAuthor = "jony";
            String diffTypes = null;
            String changeSetContext = null;
            String dataOutputDirectory = "c:\\temp";

            boolean includeCatalog = false; //Boolean.parseBoolean(getCommandParam("includeCatalog", "false"));
            boolean includeSchema = false; //Boolean.parseBoolean(getCommandParam("includeSchema", "false"));
            boolean includeTablespace = false; //Boolean.parseBoolean(getCommandParam("includeTablespace", "false"));
//            DiffOutputControl diffOutputControl = new DiffOutputControl(includeCatalog, includeSchema, includeTablespace);
//            try {
////                DiffToChangeSetLog.doGenerateChangeLog(changeLogFile, verticaDatabase, defaultCatalogName, defaultSchemaName, StringUtils.trimToNull(diffTypes), StringUtils.trimToNull(changeSetAuthor), StringUtils.trimToNull(changeSetContext), StringUtils.trimToNull(dataOutputDirectory), diffOutputControl);
////                CommandLineUtils.doDiffToChangeLog(changeLogFile, verticaDatabase, verticaDatabase1, diffOutputControl, "tables,projections");
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
