package liquibase.ext.vertica.diff.compare;

import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.compare.DatabaseObjectComparator;
import liquibase.diff.compare.DatabaseObjectComparatorChain;
import liquibase.diff.compare.DatabaseObjectComparatorFactory;
import liquibase.diff.compare.core.DefaultDatabaseObjectComparator;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.structure.Projection;
import liquibase.structure.DatabaseObject;

import java.util.Set;

/**
 * Created by vesterma on 11/03/14.
 */
public class ProjectionComparator implements DatabaseObjectComparator {
    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase){
            if (Projection.class.isAssignableFrom(objectType)) {
                return PRIORITY_TYPE;
            }
        }
        return PRIORITY_NONE;
    }

    @Override
    public String[] hash(DatabaseObject databaseObject, Database accordingTo, DatabaseObjectComparatorChain chain) {
        return chain.hash(databaseObject, accordingTo);
    }

    @Override
    public boolean isSameObject(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, DatabaseObjectComparatorChain chain) {
        if (!(databaseObject1 instanceof Projection && databaseObject2 instanceof Projection)) {
            return false;
        }

        //short circut chain.isSameObject for performance reasons. There can be a lot of tables in a database and they are compared a lot
        if (!DefaultDatabaseObjectComparator.nameMatches(databaseObject1, databaseObject2, accordingTo)) {
            return false;
        }

        if (!DatabaseObjectComparatorFactory.getInstance().isSameObject(databaseObject1.getSchema(), databaseObject2.getSchema(), chain.getSchemaComparisons(), accordingTo)) {
            return false;
        }


        return true;
    }


    @Override
    public ObjectDifferences findDifferences(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, CompareControl compareControl, DatabaseObjectComparatorChain chain, Set<String> exclude) {
        exclude.add("indexes");
        exclude.add("name");
        exclude.add("outgoingForeignKeys");
        exclude.add("uniqueConstraints");
        exclude.add("primaryKey");
        exclude.add("columns");

        ObjectDifferences differences = chain.findDifferences(databaseObject1, databaseObject2, accordingTo, compareControl, exclude);
        differences.compare("name", databaseObject1, databaseObject2, new ObjectDifferences.DatabaseObjectNameCompareFunction(Projection.class, accordingTo));

        return differences;
    }
}
