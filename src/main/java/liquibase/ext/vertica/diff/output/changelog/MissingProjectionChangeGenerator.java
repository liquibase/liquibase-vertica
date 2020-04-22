package liquibase.ext.vertica.diff.output.changelog;

import liquibase.change.Change;
import liquibase.change.ConstraintsConfig;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.AbstractChangeGenerator;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.MissingObjectChangeGenerator;
import liquibase.ext.vertica.change.ColumnConfigVertica;
import liquibase.ext.vertica.change.CreateProjectionChange;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.structure.ColumnVertica;
import liquibase.ext.vertica.structure.Projection;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;

import java.util.Date;

/**
 * Created by vesterma on 08/01/14.
 */
public class MissingProjectionChangeGenerator extends AbstractChangeGenerator implements MissingObjectChangeGenerator {
    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase)
            if (Projection.class.isAssignableFrom(objectType)) {
                return PRIORITY_DATABASE;
            }
        return PRIORITY_NONE;
    }

    @Override
    public Class<? extends DatabaseObject>[] runAfterTypes() {
        return new Class[] { Table.class };
    }

    @Override
    public Class<? extends DatabaseObject>[] runBeforeTypes() {
        return new Class[] { Column.class };
    }

    @Override
    public Change[] fixMissing(DatabaseObject missingObject, DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        Projection missingProjection = (Projection) missingObject;

//        PrimaryKey primaryKey = missingTable.getPrimaryKey();

//        if (control.diffResult.getReferenceSnapshot().getDatabase().isLiquibaseTable(missingTable.getSchema().toCatalogAndSchema(), missingTable.getName())) {
//            continue;
//        }

        CreateProjectionChange change = new CreateProjectionChange();
        change.setProjectionName(missingProjection.getName());
        change.setSchemaName(missingProjection.getSchema().getName());
		change.setSubquery(missingProjection.getSubquery());
        if (missingProjection.getOrderBy() != null) change.setOrderby(missingProjection.getOrderBy());
        if (missingProjection.getKSafe()!= null) change.setKsafe(missingProjection.getKSafe());
        if  (missingProjection.getIsSegmented()) {
            change.setSegmentedby(missingProjection.getSegmentedBy());
        }
        change.setNodes(missingProjection.getNodes());
//        if (missingProjection.getSegmentation()!= null) change.setSegmentation(missingProjection.getSegmentation());
//        if (missingProjection.getSegmentedBy()!= null) change.setSegmentedby(missingProjection.getSegmentedBy());
//        if (missingProjection.getOffset() != null) change.setOffset(missingProjection.getOffset());

        if (missingProjection.getRemarks() != null) {
            change.setRemarks(missingProjection.getRemarks());
        }

        for (Column column : missingProjection.getColumns()) {
            ColumnConfigVertica columnConfig = new ColumnConfigVertica();
            columnConfig.setName(column.getName());
            columnConfig.setType(DataTypeFactory.getInstance().from(column.getType(),comparisonDatabase).toDatabaseDataType(referenceDatabase).toString());

            if (column.isAutoIncrement()) {
                columnConfig.setAutoIncrement(true);
            }
            columnConfig.setEncoding(((ColumnVertica) column).getEncoding());

            ConstraintsConfig constraintsConfig = null;
            // In MySQL, the primary key must be specified at creation for an autoincrement column
            /*if (column.isAutoIncrement() && primaryKey != null && primaryKey.getColumnNamesAsList().contains(column.getName())) {
                constraintsConfig = new ConstraintsConfig();
                constraintsConfig.setPrimaryKey(true);
                constraintsConfig.setPrimaryKeyTablespace(primaryKey.getTablespace());
                // MySQL sets some primary key names as PRIMARY which is invalid
                if (comparisonDatabase instanceof MySQLDatabase && "PRIMARY".equals(primaryKey.getName())) {
                    constraintsConfig.setPrimaryKeyName(null);
                } else  {
                    constraintsConfig.setPrimaryKeyName(primaryKey.getName());
                }
                control.setAlreadyHandledMissing(primaryKey);
                control.setAlreadyHandledMissing(primaryKey.getBackingIndex());
            } else if (column.isNullable() != null && !column.isNullable()) {
                constraintsConfig = new ConstraintsConfig();
                constraintsConfig.setNullable(false);
            }*/

            if (constraintsConfig != null) {
                columnConfig.setConstraints(constraintsConfig);
            }

            Object defaultValue = column.getDefaultValue();
            if (defaultValue == null) {
                // do nothing
            } else if (column.isAutoIncrement()) {
                // do nothing
            } else if (defaultValue instanceof Date) {
                columnConfig.setDefaultValueDate((Date) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                columnConfig.setDefaultValueBoolean(((Boolean) defaultValue));
            } else if (defaultValue instanceof Number) {
                columnConfig.setDefaultValueNumeric(((Number) defaultValue));
            } else if (defaultValue instanceof DatabaseFunction) {
                columnConfig.setDefaultValueComputed((DatabaseFunction) defaultValue);
            } else {
                columnConfig.setDefaultValue(defaultValue.toString());
            }

            if (column.getRemarks() != null) {
                columnConfig.setRemarks(column.getRemarks());
            }

            change.addColumn(columnConfig);
            control.setAlreadyHandledMissing(column);
        }


        return new Change[] {
                change
        };
    }

    @Override
    public Change[] fixSchema(Change[] changes, CompareControl.SchemaComparison[] schemaComparisons) {
        return new Change[0];
    }

    @Override
    public Change[] fixOutputAsSchema(Change[] changes, CompareControl.SchemaComparison[] schemaComparisons) {
        return new Change[0];
    }
}
