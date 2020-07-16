package liquibase.ext.vertica.snapshot;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.jvm.UniqueConstraintSnapshotGenerator;
import liquibase.structure.DatabaseObject;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 11/11/13
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */
public class UniqueConstraintSnapshotGeneratorVertica extends UniqueConstraintSnapshotGenerator{
    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase)
            return PRIORITY_DATABASE;
        return PRIORITY_DEFAULT;
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException {
        System.out.println("in snapshot");
        return null;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException {
        System.out.println("in addTo");

    }
}
