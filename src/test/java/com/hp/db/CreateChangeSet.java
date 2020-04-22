package com.hp.db;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorFactory;
import liquibase.diff.output.changelog.core.MissingColumnChangeGenerator;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.snapshot.jvm.UniqueConstraintSnapshotGenerator;
import liquibase.util.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 10/11/13
 * Time: 21:47
 * To change this template use File | Settings | File Templates.
 */
public class CreateChangeSet {
    public static void main(String[] args){

        VerticaDatabase verticaDatabase = new VerticaDatabase();
        liquibase.database.DatabaseFactory.getInstance().register(verticaDatabase);
        liquibase.snapshot.SnapshotGeneratorFactory.getInstance().unregister(UniqueConstraintSnapshotGenerator.class);
        ChangeGeneratorFactory.getInstance().unregister(MissingColumnChangeGenerator.class);
        Properties myProp = new Properties();

        String defaultCatalogName = "yaron_secondary";
        String defaultSchemaName = defaultCatalogName;
        String password = defaultCatalogName + "_123";
        String changeLogFilePath = "c:\\liquibaseChangeSet.xml";
        String dataOutputDirectory = "c:\\liquibase_output";
        String connectionString = "jdbc:vertica://mydphdb0082.hpswlabs.adapps.hp.com:5433/eummobile";
        String changeSetAuthor = "jony";

        myProp.put("user", defaultCatalogName);
        myProp.put("password", password);

        Connection conn = null;
        try {
            File changeLogFile = new File(changeLogFilePath);
            changeLogFile.delete();

            conn = DriverManager.getConnection(connectionString, myProp);
            conn.setAutoCommit(false);
            DatabaseConnection dc = new JdbcConnection(conn);
            verticaDatabase.setConnection(dc);
            String diffTypes = null;
            String changeSetContext = null;
            boolean includeCatalog = false; //Boolean.parseBoolean(getCommandParam("includeCatalog", "false"));
            boolean includeSchema = false; //Boolean.parseBoolean(getCommandParam("includeSchema", "false"));
            boolean includeTablespace = false; //Boolean.parseBoolean(getCommandParam("includeTablespace", "false"));
            DiffOutputControl diffOutputControl = new DiffOutputControl(includeCatalog, includeSchema, includeTablespace, null);
            CommandLineUtils.doGenerateChangeLog(changeLogFilePath, verticaDatabase, defaultCatalogName, defaultSchemaName, StringUtils.trimToNull(diffTypes), StringUtils.trimToNull(changeSetAuthor), StringUtils.trimToNull(changeSetContext), StringUtils.trimToNull(dataOutputDirectory), diffOutputControl);
            postChangeSetGenerationProcessing(changeLogFile);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Adjust file to our needs.
     * TODO: to be removed after the liquibase infrastructure will handle this
     * @param changeLogFile
     */
    private static void postChangeSetGenerationProcessing(File changeLogFile) throws IOException {

        //replace column tag to vert:columnv:
        String fileContent = FileUtils.readFileToString(changeLogFile);

        fileContent = fileContent.replaceAll("<column","<vert:columnv");
        fileContent = fileContent.replaceAll("/column>","/vert:columnv>");
        fileContent = fileContent.replaceAll("<createTable","<vert:createTable");
        fileContent = fileContent.replaceAll("/createTable>","/vert:createTable>");
        fileContent = fileContent.replaceAll("<createProjection","<vert:createProjection");
        fileContent = fileContent.replaceAll("/createProjection>","/vert:createProjection>");

        String xsdName = "<databaseChangeLog xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n" +
                "                   xmlns:vert=\"http://www.liquibase.org/xml/ns/dbchangelog-ext\"\n" +
                "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "                   xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n" +
                "                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.0.xsd\n" +
                "                   http://www.liquibase.org/xml/ns/dbchangelog-ext\n" +
                "                   ../../java/liquibase/ext/vertica/xml/dbchangelog-ext.xsd \">";


        fileContent = fileContent.replaceAll("<databaseChangeLog.*>",xsdName);

        fileContent = fileContent.replaceAll("BOOLEAN\\(1\\)","BOOLEAN");


        //remove auto generated part in id's to hold only the suffix number:
        //f.i: instead of:
        //old: <changeSet author="jony" id="1392901179646-26">
        //new: <changeSet author="jony" id="26">
        final Pattern pattern = Pattern.compile("id=\"(.+?)-\\d\"");
        final Matcher matcher = pattern.matcher(fileContent);
        matcher.find();
        String generatedId = matcher.group(1);
        fileContent = fileContent.replaceAll(generatedId + "-", "");


        FileUtils.writeStringToFile(changeLogFile, fileContent, false);

    }
}
