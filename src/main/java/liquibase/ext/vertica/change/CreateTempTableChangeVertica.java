package liquibase.ext.vertica.change;

import liquibase.change.*;
import liquibase.change.core.DropTableChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.statement.CreateTableStatementVertica;
import liquibase.ext.vertica.statement.CreateTempTableStatementVertica;
import liquibase.ext.vertica.structure.Segmentation;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.*;
import liquibase.statement.core.SetColumnRemarksStatement;
import liquibase.statement.core.SetTableRemarksStatement;
import liquibase.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vesterma on 8/4/2015.
 */
@DatabaseChange(name="createTempTable", description = "create temporary tables", priority = ChangeMetaData.PRIORITY_DATABASE)
public class CreateTempTableChangeVertica extends AbstractChange implements ChangeWithColumns<ColumnConfig> {

    private String schemaName;
    private String tablespace;
    private String remarks;
    private String tableName;
    private Boolean isGlobal;
    private Boolean isPreserve;
    private String projectionName;
    private String viewName;
    private String columnAliases;
    private String objectType;
    private Boolean reducedPrecision;
    private Boolean usingIndex;
    private String tableSpace;
    private Boolean forUpdate;
    private String orderby;
    private String partitionby;
    private Integer    ksafe;
    private Segmentation segmentation;
    private String subquery;

    private List<ColumnConfig> columns;

    @Override
    public void addColumn(ColumnConfig column) {
        columns.add(column);
    }

    @Override
    @DatabaseChangeProperty(requiredForDatabase = "vertica")
    public List<ColumnConfig> getColumns() {
        if (columns == null) {
            return new ArrayList<ColumnConfig>();
        }
        return columns;
    }

    @Override
    public void setColumns(List<ColumnConfig> columns) {
        this.columns = columns;
    }

    public CreateTempTableChangeVertica() {
        super();
        columns = new ArrayList<ColumnConfig>();
    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.addAll(super.validate(database));

        if (columns != null) {
            for (ColumnConfig columnConfig : columns) {
                if (columnConfig.getType() == null) {
                    validationErrors.addError("column 'type' is required for all columns");
                }
                if (columnConfig.getName() == null) {
                    validationErrors.addError("column 'name' is required for all columns");
                }
            }
        }
        return validationErrors;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {

        CreateTempTableStatementVertica statement = new CreateTempTableStatementVertica("", getSchemaName(), getTableName());
        for (ColumnConfig column : getColumns()) {
            ConstraintsConfig constraints = column.getConstraints();
            boolean isAutoIncrement = column.isAutoIncrement() != null && column.isAutoIncrement();

            Object defaultValue = column.getDefaultValueObject();

            LiquibaseDataType columnType = DataTypeFactory.getInstance().fromDescription(column.getType() + (isAutoIncrement ? "{autoIncrement:true}" : ""),database);
            if (constraints != null && constraints.isPrimaryKey() != null && constraints.isPrimaryKey()) {

                if (column instanceof ColumnConfigVertica){
                    ColumnConfigVertica col = (ColumnConfigVertica) column;
                    statement.addPrimaryKeyColumn(column.getName(), columnType, defaultValue, constraints.getPrimaryKeyName(), col.getEncoding(),col.getAccessrank(), constraints.getPrimaryKeyTablespace());
                }else{
                    statement.addPrimaryKeyColumn(column.getName(), columnType, defaultValue, constraints.getPrimaryKeyName(), null,null, constraints.getPrimaryKeyTablespace());
                }

            } else {
                if (column instanceof ColumnConfigVertica){
                    ColumnConfigVertica col = (ColumnConfigVertica) column;
                    statement.addColumn(column.getName(),
                            columnType,
                            defaultValue,col.getEncoding(),col.getAccessrank(),null);
                }else{
                    statement.addColumn(column.getName(),
                            columnType,
                            defaultValue,column.getEncoding(), (String) null, null);
                }
            }


            if (constraints != null) {
                if (constraints.isNullable() != null && !constraints.isNullable()) {
                    statement.addColumnConstraint(new NotNullConstraint(column.getName()));
                }

                if (constraints.getReferences() != null ||
                        (constraints.getReferencedTableName() != null && constraints.getReferencedColumnNames() != null)) {
                    if (StringUtil.trimToNull(constraints.getForeignKeyName()) == null) {
                        throw new UnexpectedLiquibaseException("createTable with references requires foreignKeyName");
                    }
                    ForeignKeyConstraint fkConstraint = new ForeignKeyConstraint(constraints.getForeignKeyName(),
                            constraints.getReferences(), constraints.getReferencedTableName(), constraints.getReferencedColumnNames());
                    fkConstraint.setColumn(column.getName());
                    fkConstraint.setDeleteCascade(constraints.isDeleteCascade() != null && constraints.isDeleteCascade());
                    fkConstraint.setInitiallyDeferred(constraints.isInitiallyDeferred() != null && constraints.isInitiallyDeferred());
                    fkConstraint.setDeferrable(constraints.isDeferrable() != null && constraints.isDeferrable());
                    statement.addColumnConstraint(fkConstraint);
                }

                if (constraints.isUnique() != null && constraints.isUnique()) {
                    statement.addColumnConstraint(new UniqueConstraint(constraints.getUniqueConstraintName()).addColumns(column.getName()));
                }
            }

            if (isAutoIncrement) {
                statement.addColumnConstraint(new AutoIncrementConstraint(column.getName(), column.getStartWith(), column.getIncrementBy()));
            }
        }

        statement.setTablespace(StringUtil.trimToNull(getTablespace()));
        statement.setSegmentation(getSegmentation());
        statement.setPartitionby(getPartitionby());
        statement.setOrderby(getOrderby());
        statement.setKsafe(getKsafe());
        statement.setIsGlobal(getIsGlobal());
        statement.setIsPreserve(getIsPreserve());

        List<SqlStatement> statements = new ArrayList<SqlStatement>();
        statements.add(statement);

        if (StringUtil.trimToNull(remarks) != null) {
            SetTableRemarksStatement remarksStatement = new SetTableRemarksStatement("", schemaName, tableName, remarks);
            if (SqlGeneratorFactory.getInstance().supports(remarksStatement, database)) {
                statements.add(remarksStatement);
            }
        }

        for (ColumnConfig column : getColumns()) {
            String columnRemarks = StringUtil.trimToNull(column.getRemarks());
            if (columnRemarks != null) {
                SetColumnRemarksStatement remarksStatement = new SetColumnRemarksStatement("", schemaName, tableName, column.getName(), columnRemarks);
                if (SqlGeneratorFactory.getInstance().supports(remarksStatement, database)) {
                    statements.add(remarksStatement);
                }
            }
        }


        return statements.toArray(new SqlStatement[statements.size()]);
    }

    @Override
    protected Change[] createInverses() {
        DropTableChange inverse = new DropTableChange();
        inverse.setCatalogName("");
        inverse.setSchemaName(getSchemaName());
        inverse.setTableName(getTableName());

        return new Change[]{
                inverse
        };
    }


    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    @DatabaseChangeProperty()
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    public String getTablespace() {
        return tablespace;
    }

    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String getConfirmationMessage() {
        return "Table " + tableName + " created";
    }

    @Override
    public boolean supports(Database database) {
        return database instanceof VerticaDatabase;
    }



    public ColumnConfigVertica createColumnv(){
        ColumnConfigVertica col = new ColumnConfigVertica();
        columns.add(col);
        return col;
    }

    public Segmentation createSegmentation(){
        segmentation = new Segmentation();
        return segmentation;
    }

    public String getOrderby() {
        return orderby;
    }

    public void setOrderby(String orderby) {
        this.orderby = orderby;
    }

    public String getPartitionby() {
        return partitionby;
    }

    public void setPartitionby(String partitionby) {
        this.partitionby = partitionby;
    }

    public Integer getKsafe() {
        return ksafe;
    }

    public void setKsafe(Integer ksafe) {
        this.ksafe = ksafe;
    }

    public Segmentation getSegmentation() {
        return segmentation;
    }

    public String getSubquery() {
        return subquery;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public Boolean getIsPreserve() {
        return isPreserve;
    }

    public void setIsPreserve(Boolean isPreserve) {
        this.isPreserve = isPreserve;
    }
}
