package liquibase.ext.vertica.change;

import liquibase.change.*;
import liquibase.change.core.AddColumnChange;
import liquibase.database.Database;
import liquibase.ext.vertica.statement.AddVerticaColumnStatement;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.*;
import liquibase.statement.core.SetColumnRemarksStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by vesterma on 10/12/13.
 */
@DatabaseChange(name="addVerticaColumn", description = "add a column to an existing table", priority = ChangeMetaData.PRIORITY_DATABASE)
public class AddVerticaColumnChange extends AddColumnChange {

    public ColumnConfigVertica createColumnv(){
        ColumnConfigVertica col = new ColumnConfigVertica();
        addColumn( col);
        return col;
    }


    @Override
    public SqlStatement[] generateStatements(Database database) {

        List<SqlStatement> sql = new ArrayList<SqlStatement>();

        if (getColumns().size() == 0) {
            return new SqlStatement[] {
                    new AddVerticaColumnStatement("", getSchemaName(), getTableName(), null, null, null,null,null)
            };
        }

        for (AddColumnConfig colum : getColumns()) {
//            ColumnConfigVertica column = (ColumnConfigVertica) colum;
            AddColumnConfig column =  colum;

            Set<ColumnConstraint> constraints = new HashSet<ColumnConstraint>();
            ConstraintsConfig constraintsConfig =column.getConstraints();
            if (constraintsConfig != null) {
                if (constraintsConfig.isNullable() != null && !constraintsConfig.isNullable()) {
                    constraints.add(new NotNullConstraint());
                }
                if (constraintsConfig.isUnique() != null && constraintsConfig.isUnique()) {
                    constraints.add(new UniqueConstraint());
                }
                if (constraintsConfig.isPrimaryKey() != null && constraintsConfig.isPrimaryKey()) {
                    constraints.add(new PrimaryKeyConstraint(constraintsConfig.getPrimaryKeyName()));
                }

                if (constraintsConfig.getReferences() != null ||
                        (constraintsConfig.getReferencedColumnNames() != null && constraintsConfig.getReferencedTableName() != null)) {
                    constraints.add(new ForeignKeyConstraint(constraintsConfig.getForeignKeyName(), constraintsConfig.getReferences()
                            , constraintsConfig.getReferencedTableName(), constraintsConfig.getReferencedColumnNames()));
                }
            }

            if (column.isAutoIncrement() != null && column.isAutoIncrement()) {
                constraints.add(new AutoIncrementConstraint(column.getName(), column.getStartWith(), column.getIncrementBy()));
            }

            AddVerticaColumnStatement addColumnStatement = new AddVerticaColumnStatement(getCatalogName(), getSchemaName(),
                    getTableName(),
                    column.getName(),
                    column.getType(),
                    column.getDefaultValueObject(),
                    column.getEncoding(),
                    null, //column.getAccessrank(), TODO: add support for accessrank
                    constraints.toArray(new ColumnConstraint[constraints.size()]));

            sql.add(addColumnStatement);


            if (column.getValueObject() != null) {
                UpdateStatement updateStatement = new UpdateStatement(getCatalogName(), getSchemaName(), getTableName());
                updateStatement.addNewColumnValue(column.getName(), column.getValueObject());
                sql.add(updateStatement);
            }
        }

        for (ColumnConfig column : getColumns()) {
            String columnRemarks = StringUtil.trimToNull(column.getRemarks());
            if (columnRemarks != null) {
                SetColumnRemarksStatement remarksStatement = new SetColumnRemarksStatement("", getSchemaName(), getTableName(), column.getName(), columnRemarks);
                if (SqlGeneratorFactory.getInstance().supports(remarksStatement, database)) {
                    sql.add(remarksStatement);
                }
            }
        }

        return sql.toArray(new SqlStatement[sql.size()]);
    }

}
