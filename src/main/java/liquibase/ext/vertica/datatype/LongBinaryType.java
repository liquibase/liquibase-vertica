package liquibase.ext.vertica.datatype;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.util.StringUtil;

/**
 * Created by vesterma on 23/01/14.
 */
@DataTypeInfo(name="long varbinary", aliases = {"longblob", "longvarbinary", "java.sql.Types.BLOB", "java.sql.Types.LONGBLOB", "java.sql.Types.LONGVARBINARY",  "blob","LONG VARBINARY"}, minParameters = 0, maxParameters = 0, priority = LiquibaseDataType.PRIORITY_DATABASE)
public class LongBinaryType extends LiquibaseDataType {
    private String originalDefinition;

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        String originalDefinition = StringUtil.trimToEmpty(this.originalDefinition);

        if (database instanceof VerticaDatabase) {
            return new DatabaseDataType("LONG VARBINARY", getParameters());
        }

        return super.toDatabaseDataType(database);
    }
    @Override
    public void finishInitialization(String originalDefinition) {
        super.finishInitialization(originalDefinition);
        this.originalDefinition = originalDefinition;
    }

    @Override
    public boolean supports(Database database) {
        if (database instanceof VerticaDatabase)
            return true;
        return false;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.BLOB;
    }
}
