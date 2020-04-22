package liquibase.ext.vertica.database;

import liquibase.CatalogAndSchema;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.vertica.statement.GetProjectionDefinitionStatement;
import liquibase.logging.LogFactory;
import liquibase.statement.core.GetViewDefinitionStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Table;
import liquibase.util.StringUtils;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 10/11/13
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class VerticaDatabase extends  AbstractJdbcDatabase{

        public static final String PRODUCT_NAME = "Vertica Database";

        private Set<String> systemTablesAndViews = new HashSet<String>();

        private static Pattern INITIAL_COMMENT_PATTERN = Pattern.compile("^/\\*.*?\\*/");
        private static Pattern CREATE_PROJECTION_AS_PATTERN = Pattern.compile("(?im)^\\s*(CREATE|ALTER)\\s+?PROJECTION\\s+?((\\S+?)|(\\[.*\\])|(\\\".*\\\"))\\s+?AS\\s*?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        private static Pattern CREATE_VIEW_AS_PATTERN = Pattern.compile("^CREATE\\s+.*?VIEW\\s+.*?AS\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    private Set<String> reservedWords = new HashSet<String>();

        public VerticaDatabase() {
            super.setCurrentDateTimeFunction("NOW()");
            //got list from http://www.postgresql.org/docs/9.1/static/sql-keywords-appendix.html?
            /*reservedWords.addAll(Arrays.asList("ALL","ANALYSE", "AND", "ANY","ARRAY","AS", "ASC","ASYMMETRIC", "AUTHORIZATION", "BINARY", "BOTH","CASE","CAST","CHECK", "COLLATE","COLUMN","CONCURRENTLY", "CONSTRAINT", "CREATE", "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "DEFAULT", "DEFERRABLE", "DESC", "DISTINCT", "DO",
                    "ELSE", "END", "EXCEPT", "FALSE", "FETCH", "FOR", "FOREIGN", "FROM", "FULL", "GRANT", "GROUP", "HAVING", "ILIKE", "IN", "INITIALLY", "INTERSECT", "INTO", "IS", "ISNULL", "JOIN", "LEADING", "LIKE", "LIMIT", "LOCALTIME", "LOCALTIMESTAMP", "NOT", "NULL", "OFFSET", "ON", "ONLY", "OR", "ORDER", "OUTER", "OVER", "OVERLAPS",
                    "PLACING", "PRIMARY", "REFERENCES", "RETURNING", "RIGHT", "SELECT", "SESSION_USER", "SIMILAR", "SOME", "SYMMETRIC", "TABLE", "THEN", "TO", "TRAILING", "TRUE", "UNION", "UNIQUE", "USER", "USING", "VARIADIC", "VERBOSE", "WHEN", "WHERE", "WINDOW", "WITH"));
            super.sequenceNextValueFunction = "nextval('%s')";
            super.sequenceCurrentValueFunction = "currval('%s')";*/
            super.unmodifiableDataTypes.addAll(Arrays.asList("integer","bool","boolean", "int4", "int8", "float4", "float8", "numeric", "bigserial", "serial", "bytea", "timestamptz"));
            super.unquotedObjectsAreUppercased=false;
        }

        @Override
        public String getShortName() {
            return "vertica";
        }

        @Override
        protected String getDefaultDatabaseProductName() {
            return PRODUCT_NAME;
        }

        @Override
        public Integer getDefaultPort() {
            return 5433;
        }

        @Override
        public Set<String> getSystemViews() {
            return systemTablesAndViews;
        }

        @Override
        public int getPriority() {
            return PRIORITY_DATABASE;
        }

        @Override
        public boolean supportsInitiallyDeferrableColumns() {
            return false;
        }

    @Override
    public boolean supportsDropTableCascadeConstraints() {
        return true;
    }

    @Override
        public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
            System.out.println("checking for vertica");
            return PRODUCT_NAME.equalsIgnoreCase(conn.getDatabaseProductName());
        }

        @Override
        public String getDefaultDriver(String url) {
            if (url.startsWith("jdbc:vertica:")) {
                return "com.vertica.jdbc.Driver";
            }
            return null;
        }

    @Override
    public boolean supportsCatalogs() {
        return false;
    }

        @Override
        public boolean supportsCatalogInObjectName(Class<? extends DatabaseObject> type) {
            return false;
        }

        @Override
        public boolean supportsSequences() {
            return false;
        }

        @Override
        public String getDatabaseChangeLogTableName() {
            return super.getDatabaseChangeLogTableName().toUpperCase();
        }

        @Override
        public String getDatabaseChangeLogLockTableName() {
            return super.getDatabaseChangeLogLockTableName().toUpperCase();
        }


//    public void dropDatabaseObjects(String schema) throws DatabaseException {
//        try {
//            if (schema == null) {
//                schema = getConnectionUsername();
//            }
//            new Executor(this).execute(new RawSqlStatement("DROP OWNED BY " + schema));
//
//            getConnection().commit();
//
//            changeLogTableExists = false;
//            changeLogLockTableExists = false;
//            changeLogCreateAttempted = false;
//            changeLogLockCreateAttempted = false;
//
//        } catch (SQLException e) {
//            throw new DatabaseException(e);
//        }
//    }


        @Override
        public boolean isSystemObject(DatabaseObject example) {
            if (example instanceof Table) {
                if (example.getSchema() != null) {
                    if ("V_MONITOR".equals(example.getSchema().getName())
                            || "V_CATALOG".equals(example.getSchema().getName())) {
                        return true;
                    }
                }
            }
            return super.isSystemObject(example);
        }

        @Override
        public boolean supportsTablespaces() {
            return false;
        }

    @Override
    public String getAutoIncrementClause(BigInteger startWith, BigInteger incrementBy, String generationType, Boolean defaultOnNull) {
        if (startWith != null && incrementBy != null){
        return " IDENTITY("+startWith+","+incrementBy+") ";
        }
        return " AUTO_INCREMENT ";
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;
    }

    @Override
        public String getAutoIncrementClause() {
            return "";
        }

        @Override
        public boolean generateAutoIncrementStartWith(BigInteger startWith) {
            return true;
        }

        @Override
        public boolean generateAutoIncrementBy(BigInteger incrementBy) {
            return true;
        }

        @Override
        public String escapeObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
            if (hasMixedCase(objectName)) {
                return "\"" + objectName + "\"";
            } else {
                return super.escapeObjectName(objectName, objectType);
            }
        }

    @Override
    public String escapeObjectName(String catalogName, String schemaName, String objectName, Class<? extends DatabaseObject> objectType) {
        if (hasMixedCase(objectName)) {
            return "\"" + objectName + "\"";
        } else {
            return super.escapeObjectName(catalogName, schemaName, objectName, objectType);
        }
    }




        @Override
        public String correctObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
            if (objectName == null || quotingStrategy != ObjectQuotingStrategy.LEGACY) {
                return super.correctObjectName(objectName, objectType);
            }
            if (objectName.contains("-") || hasMixedCase(objectName) || startsWithNumeric(objectName) || isReservedWord(objectName)) {
                return objectName;
            } else {
                return objectName.toLowerCase();
            }
        }

    @Override
    public CatalogAndSchema correctSchema(final CatalogAndSchema schema) {
        if (schema == null) {
            return new CatalogAndSchema(null, getDefaultSchemaName());
        }
        String schemaName = StringUtils.trimToNull(schema.getSchemaName());

        if (schemaName == null) {
            schemaName = getDefaultSchemaName();
        }


        return new CatalogAndSchema(null, schemaName);
    }

        /*
        * Check if given string has case problems according to postgresql documentation.
        * If there are at least one characters with upper case while all other are in lower case (or vice versa) this string should be escaped.
        *
        * Note: This may make postgres support more case sensitive than normally is, but needs to be left in for backwards compatibility.
        * Method is public so a subclass extension can override it to always return false.
        */
        protected boolean hasMixedCase(String tableName) {
            if (tableName == null) {
                return false;
            }
            return StringUtils.hasUpperCase(tableName) && StringUtils.hasLowerCase(tableName);
        }

    @Override
    public boolean supportsRestrictForeignKeys() {
        return false;
    }

    @Override
        public boolean isReservedWord(String tableName) {
            return reservedWords.contains(tableName.toUpperCase());
        }

        /*
         * Get the current search paths
         */
        private List<String> getSearchPaths() {
            List<String> searchPaths = null;

            try {
                DatabaseConnection con = getConnection();

                if (con != null) {
                    String searchPathResult = (String) ExecutorService.getInstance().getExecutor(this).queryForObject(new RawSqlStatement("SHOW search_path"), String.class);

                    if (searchPathResult != null) {
                        String dirtySearchPaths[] = searchPathResult.split("\\,");
                        searchPaths = new ArrayList<String>();
                        for (String searchPath : dirtySearchPaths) {
                            searchPath = searchPath.trim();

                            // Ensure there is consistency ..
                            if (searchPath.equals("\"$user\"")) {
                                searchPath = "$user";
                            }

                            searchPaths.add(searchPath);
                        }
                    }

                }
            } catch (Exception e) {
                // TODO: Something?
                e.printStackTrace();
                LogFactory.getLogger().severe("Failed to get default catalog name from vertica", e);
            }

            return searchPaths;
        }

        @Override
        protected String getConnectionSchemaName() {
            DatabaseConnection connection = getConnection();
            if (connection == null) {
                return null;
            }
            try {
                ResultSet resultSet = ((JdbcConnection) connection).createStatement().executeQuery("SELECT CURRENT_SCHEMA");
                resultSet.next();
                String schema = resultSet.getString(1);
                System.out.println("schema_name: "+schema);
                return schema;
            } catch (Exception e) {
                LogFactory.getLogger().info("Error getting default schema", e);
            }
            return null;
        }

        public String executeSQL(String query) {
        DatabaseConnection connection = getConnection();
        if (connection == null) {
            return null;
        }
            StringBuilder  res = null;
        try {
            ResultSet resultSet = ((JdbcConnection) connection).createStatement().executeQuery(query);
            while (resultSet.next()){
                if (res == null){res = new StringBuilder();}
                res.append(resultSet.getString(1));
            }
            if (res!=null)
                return res.toString();
        } catch (Exception e) {
            LogFactory.getLogger().info("Error got exception when running: " + query, e);
        }
        return null;
    }

        private boolean catalogExists(String catalogName) throws DatabaseException {
            return catalogName != null && runExistsQuery(
                    "select count(*) from information_schema.schemata where catalog_name='" + catalogName + "'");
        }

        private boolean schemaExists(String schemaName) throws DatabaseException {
            return schemaName != null && runExistsQuery("select count(*) from information_schema.schemata where schema_name='" + schemaName + "'");
        }

        private boolean runExistsQuery(String query) throws DatabaseException {
            Long count = ExecutorService.getInstance().getExecutor(this).queryForLong(new RawSqlStatement(query));

            return count != null && count > 0;
        }

    public String getProjectionDefinition(CatalogAndSchema schema, String projectionName) throws DatabaseException {
        schema = correctSchema(schema);
        List<String> defLines = (List<String>) ExecutorService.getInstance().getExecutor(this).queryForList(new GetProjectionDefinitionStatement(schema.getCatalogName(), schema.getSchemaName(), projectionName), String.class);
        StringBuilder sb = new StringBuilder();
        for (String defLine : defLines) {
            sb.append(defLine);
        }
        String definition = sb.toString();

        String finalDef =definition.replaceAll("\r\n", "\n");
        finalDef = INITIAL_COMMENT_PATTERN.matcher(finalDef).replaceFirst("").trim(); //handle views that start with '/****** Script for XYZ command from SSMS  ******/'
        finalDef = CREATE_PROJECTION_AS_PATTERN.matcher(finalDef).replaceFirst("").trim();

        finalDef = finalDef.replaceAll("--.*", "").trim();

        /**handle views that end up as '(select XYZ FROM ABC);' */
        if (finalDef.startsWith("(") && (finalDef.endsWith(")") || finalDef.endsWith(");"))) {
            finalDef = finalDef.replaceFirst("^\\(", "");
            finalDef = finalDef.replaceFirst("\\);?$", "");
        }

        return finalDef;
    }

    @Override
    public String getViewDefinition(CatalogAndSchema schema, final String viewName) throws DatabaseException {
        schema = schema.customize(this);
//        String definition = (String) ExecutorService.getInstance().getExecutor(this).queryForObject(new GetViewDefinitionStatement(schema.getCatalogName(), schema.getSchemaName(), viewName), String.class);
        String definition = (String) ExecutorService.getInstance().getExecutor(this).queryForObject(new RawSqlStatement("select view_definition from views  where table_name='"+viewName+"' and table_schema='"+schema.getSchemaName()+"'"), String.class);
        if (definition == null) {
            return null;
        }
        return definition;
//        return CREATE_VIEW_AS_PATTERN.matcher(definition).replaceFirst("");
    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }
}
