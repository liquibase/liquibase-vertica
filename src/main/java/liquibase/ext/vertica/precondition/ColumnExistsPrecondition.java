package liquibase.ext.vertica.precondition;



        import liquibase.changelog.ChangeSet;
        import liquibase.changelog.DatabaseChangeLog;
        import liquibase.changelog.visitor.ChangeExecListener;
        import liquibase.database.Database;
        import liquibase.database.DatabaseConnection;
        import liquibase.database.jvm.JdbcConnection;
        import liquibase.exception.PreconditionErrorException;
        import liquibase.exception.PreconditionFailedException;
        import liquibase.exception.ValidationErrors;
        import liquibase.exception.Warnings;
        import liquibase.logging.LogFactory;
        import liquibase.parser.core.ParsedNode;
        import liquibase.parser.core.ParsedNodeException;
        import liquibase.precondition.Precondition;
        import liquibase.resource.ResourceAccessor;
        import liquibase.serializer.LiquibaseSerializable;
        import liquibase.structure.core.Column;
        import liquibase.structure.core.Schema;
        import liquibase.structure.core.Table;
        import liquibase.util.JdbcUtils;
        import liquibase.util.StringUtils;

        import java.sql.ResultSet;
        import java.sql.Statement;
        import java.util.HashMap;
        import java.util.Map;
        import java.util.Set;

/**
 * Created by cohenjo on 02/03/2016.
 * Taken from OPSA implementation - adjusted to liquibase 3.4.1
 * //TODO: make sure if this is even needed with the snapshot option.
 */


public class ColumnExistsPrecondition implements Precondition {
    private String catalogName;
    private String schemaName;
    private String tableName;
    private String columnName;
    private static Map<String,Map<String,Boolean>> columnExists ;
    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public Warnings warn(Database database) {
        return new Warnings();
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public void check(Database database, DatabaseChangeLog changeLog, ChangeSet changeSet, ChangeExecListener changeExecListener) throws PreconditionFailedException, PreconditionErrorException {
        Column example = new Column();
        if (StringUtils.trimToNull(getTableName()) != null) {
            example.setRelation(new Table().setName(getTableName()).setSchema(new Schema(getCatalogName(), getSchemaName())));
        }
        example.setName(getColumnName());
        DatabaseConnection dbConn =database.getConnection();

        // opsa
        if (columnExists==null) {
            columnExists = new HashMap<String, Map<String, Boolean>>();
            Map tableColumnsExists = new HashMap<String, Boolean>();
            createExistingColumnsMap((JdbcConnection) dbConn,tableColumnsExists);
        }else if (columnExists.get(getTableName())==null) {
            Map tableColumnsExists = new HashMap<String, Boolean>();
            createExistingColumnsMap((JdbcConnection) dbConn,tableColumnsExists);

        }else {
            if (columnExists.get(getTableName()).get(getColumnName())==null){
                LogFactory.getLogger().debug("Column found in cache :"+ getColumnName());
                throw new PreconditionFailedException("Column '" + database.escapeColumnName(catalogName, schemaName, getTableName(), getColumnName()) + "' does not exist", changeLog, this);
            }
        }

        /*try {
            if (!SnapshotGeneratorFactory.getInstance().has(example, database)) {
                throw new PreconditionFailedException("Column '" + database.escapeColumnName(catalogName, schemaName, getTableName(), getColumnName()) + "' does not exist", changeLog, this);
            }
        } catch (LiquibaseException e) {
            throw new PreconditionErrorException(e, changeLog, this);
        }*/
    }

    @Override
    public String getSerializedObjectName() {
        return null;
    }

    @Override
    public Set<String> getSerializableFields() {
        return null;
    }

    @Override
    public Object getSerializableFieldValue(String field) {
        return null;
    }

    @Override
    public SerializationType getSerializableFieldType(String field) {
        return null;
    }

    @Override
    public String getSerializableFieldNamespace(String field) {
        return null;
    }

    @Override
    public String getSerializedObjectNamespace() {
        return null;
    }

    @Override
    public void load(ParsedNode parsedNode, ResourceAccessor resourceAccessor) throws ParsedNodeException {

    }

    @Override
    public ParsedNode serialize() throws ParsedNodeException {
        return null;
    }

    private void createExistingColumnsMap(JdbcConnection dbConn,Map tableColumnsExists) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = ((JdbcConnection) dbConn).createStatement();
            rs = stmt.executeQuery("select column_name from columns where table_schema ='" + schemaName + "' and table_name='" + getTableName() + "' ");
            while (rs.next()) {
                tableColumnsExists.put(rs.getString("column_name"), Boolean.TRUE);
            }
            columnExists.put(getTableName(),tableColumnsExists);
        } catch (Exception e) {
            LogFactory.getLogger().info("Error fetching columns name from metadata ", e);
            throw new RuntimeException("Error fetching columns name from metadata ", e);
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(stmt);
        }
    }


    @Override
    public String getName() {
        return "columnExists";
    }
}
