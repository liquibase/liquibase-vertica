package liquibase.ext.vertica.snapshot;

import liquibase.CatalogAndSchema;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.snapshot.*;
import liquibase.snapshot.jvm.JdbcSnapshotGenerator;
import liquibase.snapshot.jvm.TableSnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import liquibase.util.StringUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by vesterma on 06/02/14.
 */
public class TableSnapshotGeneratorVertica extends JdbcSnapshotGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase)
            return PRIORITY_DATABASE;
        return PRIORITY_NONE;

    }


    public TableSnapshotGeneratorVertica() {
        super(Table.class, new Class[] { Schema.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Database database = snapshot.getDatabase();
        String objectName = example.getName();
        Schema schema = example.getSchema();

        List<CachedRow> rs = null;
        try {
            VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());
//            projectionMetadataRs = ((JdbcDatabaseSnapshot) snapshot).getMetaData().getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), example.getName(), new String[]{"VIEW"});
            rs = (verticaDatabaseSnapshot).getMetaData().getTables(schema.getName(), example.getName());
//            JdbcDatabaseSnapshot.CachingDatabaseMetaData metaData = ((JdbcDatabaseSnapshot) snapshot).getMetaData();
//            rs = metaData.getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), database.correctObjectName(objectName, Table.class), new String[]{"TABLE"});
//            rs = metaData.getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), database.correctObjectName(objectName, Table.class));

            Table table;
            if (rs.size() > 0) {
                table = readTable(rs.get(0), database);
            } else {
                return null;
            }

            return table;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Table.class)) {
            return;
        }

        if (foundObject instanceof Schema) {

            Database database = snapshot.getDatabase();
            Schema schema = (Schema) foundObject;

            List<CachedRow> tableMetaDataRs = null;
            try {
//                tableMetaDataRs = ((JdbcDatabaseSnapshot) snapshot).getMetaData().getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), null, new String[]{"TABLE"});
                tableMetaDataRs = ((JdbcDatabaseSnapshot) snapshot).getMetaData().getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), null);
                for (CachedRow row : tableMetaDataRs) {
                    String tableName = row.getString("TABLE_NAME");
                    Table tableExample = (Table) new Table().setName(cleanNameFromDatabase(tableName, database)).setSchema(schema);

                    schema.addDatabaseObject(tableExample);
                }
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }
        }


    }

    protected Table readTable(CachedRow tableMetadataResultSet, Database database) throws SQLException, DatabaseException {
        String rawTableName = tableMetadataResultSet.getString("TABLE_NAME");
        String rawSchemaName = StringUtils.trimToNull(tableMetadataResultSet.getString("TABLE_SCHEM"));
        String rawCatalogName = StringUtils.trimToNull(tableMetadataResultSet.getString("TABLE_CAT"));
        String remarks = StringUtils.trimToNull(tableMetadataResultSet.getString("REMARKS"));
        if (remarks != null) {
            remarks = remarks.replace("''", "'"); //come back escaped sometimes
        }

        Table table = new Table().setName(cleanNameFromDatabase(rawTableName, database));
        table.setRemarks(remarks);


        CatalogAndSchema schemaFromJdbcInfo = ((AbstractJdbcDatabase) database).getSchemaFromJdbcInfo(rawCatalogName, rawSchemaName);
        table.setSchema(new Schema(schemaFromJdbcInfo.getCatalogName(), schemaFromJdbcInfo.getSchemaName()));


        /*
        * Get the vertica specific details: ksafe, partition by, etc'
        * */

        String partitionby = ((VerticaDatabase)database).executeSQL("select partition_expression from tables where table_name ='"+table.getName()+"'");
        if((partitionby != null)&&(!partitionby.isEmpty()))
            table.setAttribute("partitionby",partitionby);



        return table;
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces(){
        return new Class[]{TableSnapshotGenerator.class};
    }


}

