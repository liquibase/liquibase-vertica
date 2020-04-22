package liquibase.ext.vertica.sqlgenerator;

import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.datatype.DatabaseDataType;
import liquibase.exception.ValidationErrors;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.statement.CreateTableStatementVertica;
import liquibase.ext.vertica.statement.CreateTempTableStatementVertica;
import liquibase.ext.vertica.structure.Segmentation;
import liquibase.logging.LogFactory;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.AutoIncrementConstraint;
import liquibase.util.StringUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by vesterma on 8/4/2015.
 */
public class CreateTempTableGeneratorVertica extends AbstractSqlGenerator<CreateTempTableStatementVertica> {
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(CreateTempTableStatementVertica statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", statement.getTableName());
        validationErrors.checkRequiredField("columns", statement.getColumns());
        return validationErrors;
    }

    @Override
    public boolean supports(CreateTempTableStatementVertica statement, Database database) {
        return database instanceof VerticaDatabase;
    }


    @Override
    public Sql[] generateSql(CreateTempTableStatementVertica statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        StringBuilder sql = new StringBuilder();

        sql.append( "CREATE ");
        if (statement.getIsGlobal() != null)
            sql.append(" GLOBAL ");
        else
            sql.append(" LOCAL ");
        sql.append( " TEMPORARY TABLE ");
        if (statement.getSchemaName() != null)
            sql.append(statement.getSchemaName()).append(".");
        else
            sql.append(database.getDefaultSchemaName()).append(".");
        if (statement.getTableName() != null) {
            sql.append(statement.getTableName())
                    .append(" ");
        }

        boolean isSinglePrimaryKeyColumn = statement.getPrimaryKeyConstraint() != null
                && statement.getPrimaryKeyConstraint().getColumns().size() == 1;

        boolean isPrimaryKeyAutoIncrement = false;

        sql.append("( ");
        Iterator<String> columnIterator = statement.getColumns().iterator();
        List<String> primaryKeyColumns = new LinkedList<String>();
        while (columnIterator.hasNext()) {
            String column = columnIterator.next();
            DatabaseDataType columnType = statement.getColumnTypes().get(column).toDatabaseDataType(database);
            sql.append(database.escapeColumnName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), column));


            // This is the difference between vertica & other RDBMS - the encoding part.



            AutoIncrementConstraint autoIncrementConstraint = null;

            for (AutoIncrementConstraint currentAutoIncrementConstraint : statement.getAutoIncrementConstraints()) {
                if (column.equals(currentAutoIncrementConstraint.getColumnName())) {
                    autoIncrementConstraint = currentAutoIncrementConstraint;
                    break;
                }
            }

            boolean isAutoIncrementColumn = autoIncrementConstraint != null;
            boolean isPrimaryKeyColumn = statement.getPrimaryKeyConstraint() != null
                    && statement.getPrimaryKeyConstraint().getColumns().contains(column);
            isPrimaryKeyAutoIncrement = isPrimaryKeyAutoIncrement
                    || isPrimaryKeyColumn && isAutoIncrementColumn;

            if (isPrimaryKeyColumn) {
                primaryKeyColumns.add(column);
            }
            if (!isAutoIncrementColumn) { sql.append(" ").append(columnType);}

            // for the serial data type in postgres, there should be no default value
            if (!columnType.isAutoIncrement() && statement.getDefaultValue(column) != null) {
                Object defaultValue = statement.getDefaultValue(column);
                sql.append(" DEFAULT ");
                sql.append(statement.getColumnTypes().get(column).objectToSql(defaultValue, database));
            }

            // TODO: Change this - vertica supports both auto incremental & identity fields.
            if (isAutoIncrementColumn) {
                // TODO: check if database supports auto increment on non primary key column
                if (database.supportsAutoIncrement()) {
                    String autoIncrementClause = database.getAutoIncrementClause(autoIncrementConstraint.getStartWith(), autoIncrementConstraint.getIncrementBy(), autoIncrementConstraint.getGenerationType(), autoIncrementConstraint.getDefaultOnNull());

                    if (!"".equals(autoIncrementClause)) {
                        sql.append(" ").append(autoIncrementClause);
                    }

                    if (database instanceof PostgresDatabase && autoIncrementConstraint.getStartWith() != null) {
                        String sequenceName = statement.getTableName()+"_"+column+"_seq";
//                        additionalSql.add(new UnparsedSql("alter sequence "+database.escapeSequenceName(statement.getCatalogName(), statement.getSchemaName(), sequenceName)+" start with "+autoIncrementConstraint.getStartWith(), new Sequence().setName(sequenceName).setSchema(statement.getCatalogName(), statement.getSchemaName())));
                    }
                } else {
                    LogFactory.getLogger().warning(database.getShortName()+" does not support autoincrement columns as request for "+(database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName())));
                }
            }

            if (isPrimaryKeyColumn) {

                String pkName = StringUtils.trimToNull(statement.getPrimaryKeyConstraint().getConstraintName());
                if (pkName != null) {
                    sql.append(" CONSTRAINT ");
                    sql.append(database.escapeConstraintName(pkName));
                }

                sql.append(" PRIMARY KEY ");
            }

            if (statement.getNotNullColumns().containsKey(column)) {
                sql.append(" NOT NULL");
            }

            if(statement.getColumnEncoding(column)!=null){
                sql.append(" ENCODING ").append(statement.getColumnEncoding(column));
            }

            if(statement.getColumnAccessrank(column) != null)
                sql.append(" ACCESSRANK  ").append(statement.getColumnAccessrank(column));

            if (columnIterator.hasNext()) {
                sql.append(", ");
            }
        }
        sql.append(" )");
        sql.append(" ON COMMIT ");
        if(statement.getIsPreserve())
            sql.append(" PRESERVE ");
        else
            sql.append(" DELETE");
        sql.append("  ROWS ");


        if(statement.getOrderby() != null)
            sql.append(" ORDER BY ").append(statement.getOrderby());

        if (statement.getSegmentation() != null){
            Segmentation seg = statement.getSegmentation();
            if (seg.getUnsegmented() == true){
                sql.append(" UNSEGMENTED ");
            }else{
                sql.append(" SEGMENTED BY ");

                sql.append(seg.getExpression());

            }
            if (seg.getAllNodes()){
                sql.append(" ALL NODES ");
            }else{
                sql.append(" NODES ").append(seg.getNodes());
                if (seg.getOffset() != null)
                    sql.append(" OFFSET ").append(seg.getOffset().toString());
            }

        }

        if (statement.getKsafe() != null)
            sql.append(" KSAFE ").append(statement.getKsafe());

        if  (statement.getPartitionby() != null)
            sql.append(" PARTITION BY ").append(statement.getPartitionby());


        System.out.println(sql.toString());
        return new Sql[]
                {
                        new UnparsedSql(sql.toString())
                };
    }
}
