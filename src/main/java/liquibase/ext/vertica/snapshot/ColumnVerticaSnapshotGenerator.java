package liquibase.ext.vertica.snapshot;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.datatype.core.*;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.structure.ColumnVertica;
import liquibase.ext.vertica.structure.Projection;
import liquibase.logging.LogFactory;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.jvm.JdbcSnapshotGenerator;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;
import liquibase.util.StringUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created by vesterma on 08/01/14.
 */
public class ColumnVerticaSnapshotGenerator extends JdbcSnapshotGenerator { //extends ColumnSnapshotGenerator  {

    public ColumnVerticaSnapshotGenerator() {
//        super(Column.class, new Class[]{Table.class, View.class});
        super(ColumnVertica.class, new Class[]{ Table.class,Projection.class}); //todo: add: Table.class, once the replace functionality works

    }

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase)
            return PRIORITY_DATABASE;
        return PRIORITY_NONE;

    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Database database = snapshot.getDatabase();
        Relation relation = ((Column) example).getRelation();
        Schema schema = relation.getSchema();

        List<CachedRow> columnMetadataRs = null;
        try {
            if (relation instanceof Projection) {
    //            JdbcDatabaseSnapshot.CachingDatabaseMetaData databaseMetaData = ((JdbcDatabaseSnapshot) snapshot).getMetaData();
                VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());
    //            columnMetadataRs = databaseMetaData.getColumns(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), relation.getName(), example.getName());
                columnMetadataRs = (verticaDatabaseSnapshot.getMetaData().getProjectionColumns(schema.getName(), relation.getName(), example.getName())); //, table.getName()));
            }/*else{
                JdbcDatabaseSnapshot.CachingDatabaseMetaData databaseMetaData = ((JdbcDatabaseSnapshot) snapshot).getMetaData();
                columnMetadataRs = databaseMetaData.getColumns(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), relation.getName(), example.getName());
            }*/
            if (columnMetadataRs.size() > 0) {
                CachedRow data = columnMetadataRs.get(0);
                return readColumn(data, relation, database);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }

    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(ColumnVertica.class)) {
            return;
        }
        if (foundObject instanceof Projection) {
            System.out.println("in vert col addTo, found: " + foundObject.getName());
            Database database = snapshot.getDatabase();
            Projection relation = (Projection) foundObject;
            List<CachedRow> allColumnsMetadataRs = null;
            try {

//                JdbcDatabaseSnapshot.CachingDatabaseMetaData databaseMetaData = ((JdbcDatabaseSnapshot) snapshot).getMetaData();
                VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());


                Schema schema;

                schema = relation.getSchema();
//                allColumnsMetadataRs = databaseMetaData.getColumns(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), relation.getName(), null);
                allColumnsMetadataRs = (verticaDatabaseSnapshot.getMetaData().getProjectionColumns(schema.getName(), relation.getName(), null)); //, table.getName()));

                for (CachedRow row : allColumnsMetadataRs) {
                    ColumnVertica exampleColumn = new ColumnVertica();
                    exampleColumn.setRelation(relation).setName(row.getString("COLUMN_NAME"));
                    relation.getColumns().add(exampleColumn);
                }

                String orderBy = createOrderByClause(allColumnsMetadataRs);
                relation.setOrderBy(orderBy);

            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        } /*else if (foundObject instanceof Table) {
            System.out.println("in vert col addTo, found: " + foundObject.getName());
            Database database = snapshot.getDatabase();
            Table relation = (Table) foundObject;
            List<CachedRow> allColumnsMetadataRs = null;
            try {

//                VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());
                JdbcDatabaseSnapshot.CachingDatabaseMetaData databaseMetaData = ((JdbcDatabaseSnapshot) snapshot).getMetaData();

                Schema schema;
                schema = relation.getSchema();
//                allColumnsMetadataRs = (verticaDatabaseSnapshot.getMetaData().getProjectionColumns(schema.getName(), relation.getName(), null)); //, table.getName()));

                allColumnsMetadataRs = databaseMetaData.getColumns(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), relation.getName(), null);


                for (CachedRow row : allColumnsMetadataRs) {
                    ColumnVertica exampleColumn = new ColumnVertica();
                    exampleColumn.setRelation(relation).setName(row.getString("COLUMN_NAME"));
                    relation.getColumns().add(exampleColumn);
                }

            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        }*/

    }

    /**
     * Yaron Relevy added:
     * VerticaDatabaseSnapshot.verticaQuery method brings the sort_position that we create out of it the "orderby" clause
     * @param columnsMetadataRs
     */
    protected String createOrderByClause(List<CachedRow> columnsMetadataRs) {
        Map<Integer,String> sortPositionToColumnMap = new TreeMap<Integer,String>();
        //populate sortPositionToColumnMap:
        for (CachedRow row : columnsMetadataRs) {
            Integer sortPosition = row.getInt("SORT_POSITION");
            if(sortPosition != null){
                String columnName = row.getString("COLUMN_NAME");
                sortPositionToColumnMap.put(sortPosition, columnName);
            }

        }
        StringBuilder orderBy = new StringBuilder();
        for(int i = 0; i < sortPositionToColumnMap.size(); i++){
            if(i > 0){
                orderBy.append(",");
            }
            orderBy.append(sortPositionToColumnMap.get(i));
        }
        return orderBy.toString();
    }


    /*
    * code taken from ColumnSnapshotGenerator - it removed exess code and added vertica specifics
    **/
    protected ColumnVertica readColumn(CachedRow columnMetadataResultSet, Relation table, Database database) throws SQLException, DatabaseException {
        String rawProjectionName = null;
        if (table instanceof Projection){
            rawProjectionName = (String) columnMetadataResultSet.get("PROJ_NAME");
        }else{
            rawProjectionName =(String) columnMetadataResultSet.get("TABLE_NAME");
        }
        String rawColumnName = (String) columnMetadataResultSet.get("COLUMN_NAME");
        String rawSchemaName = StringUtil.trimToNull((String) columnMetadataResultSet.get("TABLE_SCHEM"));
        String rawCatalogName = StringUtil.trimToNull((String) columnMetadataResultSet.get("TABLE_CAT"));
        String remarks = StringUtil.trimToNull((String) columnMetadataResultSet.get("REMARKS"));
        if (remarks != null) {
            remarks = remarks.replace("''", "'"); //come back escaped sometimes
        }


        ColumnVertica column = new ColumnVertica();
        column.setName(rawColumnName);
        column.setRelation(table);
        column.setRemarks(remarks);


        Boolean nullable = columnMetadataResultSet.getBoolean("NULLABLE");
        if (nullable == null) {
            Scope.getCurrentScope().getLog(getClass()).info("Unknown nullable state for column " + column.toString() + ". Assuming nullable");
            column.setNullable(true);
        }else {
            column.setNullable(nullable);
        }

        String encoding = columnMetadataResultSet.getString("ENCODING");
        column.setEncoding(encoding);

        if (database.supportsAutoIncrement()) {
            if ((table instanceof Table) || (table instanceof Projection)) { // this is to proevent from entering...
                if (columnMetadataResultSet.containsColumn("IS_AUTOINCREMENT")) {
                    Boolean isAutoincrement = (Boolean) columnMetadataResultSet.get("IS_AUTOINCREMENT");
                    if (isAutoincrement == null) {
                        column.setAutoIncrementInformation(null);
                    } else if (isAutoincrement) {
                        column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                    } else if (!isAutoincrement) {
                        column.setAutoIncrementInformation(null);
                    } else {
                        throw new UnexpectedLiquibaseException("Unknown is_autoincrement value: '" + isAutoincrement+"'");
                    }
                } else {
                    //TODO: verify if we still need this.
                    //probably older version of java, need to select from the column to find out if it is auto-increment
                    String selectStatement = "select " + database.escapeColumnName(rawCatalogName, rawSchemaName, rawProjectionName, rawColumnName) + " from " + database.escapeTableName(rawCatalogName, rawSchemaName, rawProjectionName) + " where 0=1";
                    Scope.getCurrentScope().getLog(getClass()).fine("Checking "+rawProjectionName+"."+rawCatalogName+" for auto-increment with SQL: '"+selectStatement+"'");
                    Connection underlyingConnection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
                    Statement statement = null;
                    ResultSet columnSelectRS = null;

                    try {
                        statement = underlyingConnection.createStatement();
                        columnSelectRS = statement.executeQuery(selectStatement);
                        if (columnSelectRS.getMetaData().isAutoIncrement(1)) {
                            column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                        } else {
                            column.setAutoIncrementInformation(null);
                        }
                    } finally {
                        try {
                            if (statement != null) {
                                statement.close();
                            }
                        } catch (SQLException ignore) {
                        }
                        if (columnSelectRS != null) {
                            columnSelectRS.close();
                        }
                    }
                }
            }
        }

        DataType type = readDataType(columnMetadataResultSet, column, database);
        column.setType(type);

        column.setDefaultValue(readDefaultValue(columnMetadataResultSet, column, database));

        return column;
    }

    protected DataType readDataType(CachedRow columnMetadataResultSet, Column column, Database database) throws SQLException {


        String columnTypeName = (String) columnMetadataResultSet.get("TYPE_NAME");




        DataType.ColumnSizeUnit columnSizeUnit = DataType.ColumnSizeUnit.BYTE;

        int dataType = columnMetadataResultSet.getInt("DATA_TYPE");
        Integer columnSize = columnMetadataResultSet.getInt("COLUMN_SIZE");
        // don't set size for types like int4, int8 etc
        if (database.dataTypeIsNotModifiable(columnTypeName)) {
            columnSize = null;
        }

        Integer decimalDigits = columnMetadataResultSet.getInt("DECIMAL_DIGITS");
        if (decimalDigits != null && decimalDigits.equals(0)) {
            decimalDigits = null;
        }

        Integer radix = columnMetadataResultSet.getInt("NUM_PREC_RADIX");

        Integer characterOctetLength = columnMetadataResultSet.getInt("CHAR_OCTET_LENGTH");


        DataType type = new DataType(columnTypeName);
        type.setDataTypeId(dataType);
        type.setColumnSize(columnSize);
        type.setDecimalDigits(decimalDigits);
        type.setRadix(radix);
        type.setCharacterOctetLength(characterOctetLength);
        type.setColumnSizeUnit(columnSizeUnit);

        return type;
    }

    protected Object readDefaultValue(CachedRow columnMetadataResultSet, Column columnInfo, Database database) throws SQLException, DatabaseException {

        Object val = columnMetadataResultSet.get("COLUMN_DEF");
        if (!(val instanceof String)) {
            return val;
        }

        String stringVal = (String) val;
        if (stringVal.isEmpty()) {
            return null;
        }

        if (stringVal.startsWith("'") && stringVal.endsWith("'")) {
            stringVal = stringVal.substring(1, stringVal.length() - 1);
        } else if (stringVal.startsWith("((") && stringVal.endsWith("))")) {
            stringVal = stringVal.substring(2, stringVal.length() - 2);
        } else if (stringVal.startsWith("('") && stringVal.endsWith("')")) {
            stringVal = stringVal.substring(2, stringVal.length() - 2);
        } else if (stringVal.startsWith("(") && stringVal.endsWith(")")) {
            return new DatabaseFunction(stringVal.substring(1, stringVal.length() - 1));
        }

        int type = Integer.MIN_VALUE;
        if (columnInfo.getType().getDataTypeId() != null) {
            type = columnInfo.getType().getDataTypeId();
        }
        String typeName = columnInfo.getType().getTypeName();
        Scanner scanner = new Scanner(stringVal.trim());
        try {
            LiquibaseDataType liquibaseDataType = DataTypeFactory.getInstance().from(columnInfo.getType(),database);
            if (type == Types.ARRAY) {
                return new DatabaseFunction(stringVal);
            } else if ((liquibaseDataType instanceof BigIntType || type == Types.BIGINT)) {
                if (scanner.hasNextBigInteger()) {
                    return scanner.nextBigInteger();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (type == Types.BINARY) {
                return new DatabaseFunction(stringVal.trim());
            } else if (type == Types.BIT) {
                if (stringVal.startsWith("b'")) { //mysql returns boolean values as b'0' and b'1'
                    stringVal = stringVal.replaceFirst("b'", "").replaceFirst("'$", "");
                }
                stringVal = stringVal.trim();
                if (scanner.hasNextBoolean()) {
                    return scanner.nextBoolean();
                } else {
                    return new Integer(stringVal);
                }
            } else if (liquibaseDataType instanceof BlobType|| type == Types.BLOB) {
                return new DatabaseFunction(stringVal);
            } else if ((liquibaseDataType instanceof BooleanType || type == Types.BOOLEAN )) {
                if (scanner.hasNextBoolean()) {
                    return scanner.nextBoolean();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (liquibaseDataType instanceof CharType || type == Types.CHAR) {
                return stringVal;
            } else if (liquibaseDataType instanceof ClobType || type == Types.CLOB) {
                return stringVal;
            } else if (type == Types.DATALINK) {
                return new DatabaseFunction(stringVal);
            } else if (liquibaseDataType instanceof DateType || type == Types.DATE) {
                if (typeName.equalsIgnoreCase("year")) {
                    return stringVal.trim();
                }
                if (zeroTime(stringVal)) {
                    return stringVal;
                }
                return new java.sql.Date(getDateFormat(database).parse(stringVal.trim()).getTime());
            } else if ((liquibaseDataType instanceof DecimalType || type == Types.DECIMAL)) {
                if (scanner.hasNextBigDecimal()) {
                    return scanner.nextBigDecimal();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (type == Types.DISTINCT) {
                return new DatabaseFunction(stringVal);
            } else if ((liquibaseDataType instanceof DoubleType || type == Types.DOUBLE)) {
                if (scanner.hasNextDouble()) {
                    return scanner.nextDouble();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if ((liquibaseDataType instanceof FloatType || type == Types.FLOAT)) {
                if (scanner.hasNextFloat()) {
                    return scanner.nextFloat();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if ((liquibaseDataType instanceof IntType || type == Types.INTEGER)) {
                if (scanner.hasNextInt()) {
                    return scanner.nextInt();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (type == Types.JAVA_OBJECT) {
                return new DatabaseFunction(stringVal);
            } else if (type == Types.LONGNVARCHAR) {
                return stringVal;
            } else if (type == Types.LONGVARBINARY) {
                return new DatabaseFunction(stringVal);
            } else if (type == Types.LONGVARCHAR) {
                return stringVal;
            } else if (liquibaseDataType instanceof NCharType || type == Types.NCHAR) {
                return stringVal;
            } else if (type == Types.NCLOB) {
                return stringVal;
            } else if (type == Types.NULL) {
                return null;
            } else if ((liquibaseDataType instanceof NumberType || type == Types.NUMERIC)) {
                if (scanner.hasNextBigDecimal()) {
                    return scanner.nextBigDecimal();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (liquibaseDataType instanceof NVarcharType || type == Types.NVARCHAR) {
                return stringVal;
            } else if (type == Types.OTHER) {
                return new DatabaseFunction(stringVal);
            } else if (type == Types.REAL) {
                return new BigDecimal(stringVal.trim());
            } else if (type == Types.REF) {
                return new DatabaseFunction(stringVal);
            } else if (type == Types.ROWID) {
                return new DatabaseFunction(stringVal);
            } else if ((liquibaseDataType instanceof SmallIntType || type == Types.SMALLINT)) {
                if (scanner.hasNextInt()) {
                    return scanner.nextInt();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (type == Types.SQLXML) {
                return new DatabaseFunction(stringVal);
            } else if (type == Types.STRUCT) {
                return new DatabaseFunction(stringVal);
            } else if (liquibaseDataType instanceof TimeType || type == Types.TIME) {
                if (zeroTime(stringVal)) {
                    return stringVal;
                }
                return new java.sql.Time(getTimeFormat(database).parse(stringVal).getTime());
            } else if (liquibaseDataType instanceof DateTimeType || liquibaseDataType instanceof TimestampType || type == Types.TIMESTAMP) {
                if (zeroTime(stringVal)) {
                    return stringVal;
                }
                return new Timestamp(getDateTimeFormat(database).parse(stringVal).getTime());
            } else if ((liquibaseDataType instanceof TinyIntType || type == Types.TINYINT)) {
                if (scanner.hasNextInt()) {
                    return scanner.nextInt();
                } else {
                    return new DatabaseFunction(stringVal);
                }
            } else if (type == Types.VARBINARY) {
                return new DatabaseFunction(stringVal);
            } else if (liquibaseDataType instanceof VarcharType || type == Types.VARCHAR) {
                return stringVal;
            }  else {
                Scope.getCurrentScope().getLog(getClass()).info("Unknown default value: value '" + stringVal + "' type " + typeName + " (" + type + "), assuming it is a function");
                return new DatabaseFunction(stringVal);
            }
        } catch (ParseException e) {
            return new DatabaseFunction(stringVal);
        }
    }

    private boolean zeroTime(String stringVal) {
        return stringVal.replace("-","").replace(":", "").replace(" ","").replace("0","").equals("");
    }

    protected DateFormat getDateFormat(Database database) {
        return new SimpleDateFormat("yyyy-MM-dd");

    }

    protected DateFormat getTimeFormat(Database database) {
        return new SimpleDateFormat("HH:mm:ss");
    }

    protected DateFormat getDateTimeFormat(Database database) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    /*@Override
    public Class<? extends SnapshotGenerator>[] replaces(){
        return new Class[]{ColumnSnapshotGenerator.class};
    }*/

}
