package liquibase.ext.vertica.diff.output.changelog;

import liquibase.CatalogAndSchema;
import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.*;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.DatabaseObject;
import liquibase.structure.DatabaseObjectComparator;
import liquibase.util.StringUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vesterma on 27/04/2014.
 */
public class DiffToChangeSetLog extends DiffToChangeLog {

    private String changeSetContext;
    private DiffResult diffResult;
    private DiffOutputControl diffOutputControl;

    public DiffToChangeSetLog(DiffResult diffResult, DiffOutputControl diffOutputControl) {
        super(diffResult, diffOutputControl);
        this.diffResult = diffResult;
        this.diffOutputControl = diffOutputControl;

    }

    public DiffToChangeSetLog(DiffOutputControl diffOutputControl) {
        super(diffOutputControl);
        this.diffOutputControl = diffOutputControl;
    }

    @Override
    public void setChangeSetContext(String changeSetContext) {
        this.changeSetContext = changeSetContext;
    }

    @Override
    public List<ChangeSet> generateChangeSets() {
        final ChangeGeneratorFactory changeGeneratorFactory = ChangeGeneratorFactory.getInstance();
        DatabaseObjectComparator comparator = new DatabaseObjectComparator();

        List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
        ObjectQuotingStrategy quotingStrategy = ObjectQuotingStrategy.QUOTE_ALL_OBJECTS;
        ChangeSet changeSet = new ChangeSet(generateId(null), getChangeSetAuthor(), false, false,null, changeSetContext, null, quotingStrategy, null);
        List<Class<? extends DatabaseObject>> types = getOrderedOutputTypes(MissingObjectChangeGenerator.class);
        for (Class<? extends DatabaseObject> type : types) {
            for (DatabaseObject object : diffResult.getMissingObjects(type, comparator)) {
                if (object == null) {
                    continue;
                }
                Change[] changes = changeGeneratorFactory.fixMissing(object, diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
                if (!diffResult.getReferenceSnapshot().getDatabase().isLiquibaseObject(object) && !diffResult.getReferenceSnapshot().getDatabase().isSystemObject(object)) {
                    addToChangeSet(changes, changeSet);
                }
            }
        }

        types = getOrderedOutputTypes(UnexpectedObjectChangeGenerator.class);
        for (Class<? extends DatabaseObject> type : types) {
            for (DatabaseObject object : diffResult.getUnexpectedObjects(type, comparator)) {
                Change[] changes = changeGeneratorFactory.fixUnexpected(object, diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
                if (!diffResult.getComparisonSnapshot().getDatabase().isLiquibaseObject(object) && !diffResult.getComparisonSnapshot().getDatabase().isSystemObject(object)) {
                    addToChangeSet(changes, changeSet);
                }
            }
        }

        types = getOrderedOutputTypes(ChangedObjectChangeGenerator.class);
        for (Class<? extends DatabaseObject> type : types) {
            for (Map.Entry<? extends DatabaseObject, ObjectDifferences> entry : diffResult.getChangedObjects(type, comparator).entrySet()) {
                Change[] changes = changeGeneratorFactory.fixChanged(entry.getKey(), entry.getValue(), diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
                if (!diffResult.getReferenceSnapshot().getDatabase().isLiquibaseObject(entry.getKey()) && !diffResult.getReferenceSnapshot().getDatabase().isSystemObject(entry.getKey())) {
                    addToChangeSet(changes, changeSet);
                }
            }
        }
        changeSets.add(changeSet);
        return changeSets;
    }

    private void addToChangeSet(Change[] changes, ChangeSet changeSet) {
        if (changes != null) {
            for (Change change : changes) {
                changeSet.addChange(change);
            }
        }
    }


    public static void doGenerateChangeLog(String changeLogFile, Database originalDatabase, String catalogName, String schemaName, String snapshotTypes, String author, String context, String dataDir, DiffOutputControl diffOutputControl) throws DatabaseException, IOException, ParserConfigurationException, InvalidExampleException {
        SnapshotControl snapshotControl = new SnapshotControl(originalDatabase, snapshotTypes);
        CompareControl compareControl = new CompareControl(new CompareControl.SchemaComparison[] {new CompareControl.SchemaComparison(new CatalogAndSchema(catalogName, schemaName), new CatalogAndSchema(catalogName, schemaName))}, snapshotTypes);
//        compareControl.addStatusListener(new OutDiffStatusListener());

        diffOutputControl.setDataDir(dataDir);

        DatabaseSnapshot originalDatabaseSnapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(compareControl.getSchemas(CompareControl.DatabaseRole.REFERENCE), originalDatabase, snapshotControl);
        DiffResult diffResult = DiffGeneratorFactory.getInstance().compare(originalDatabaseSnapshot, SnapshotGeneratorFactory.getInstance().createSnapshot(compareControl.getSchemas(CompareControl.DatabaseRole.REFERENCE), null, snapshotControl), compareControl);

        DiffToChangeLog changeLogWriter = new DiffToChangeSetLog(diffResult, diffOutputControl);

        changeLogWriter.setChangeSetAuthor(author);
        changeLogWriter.setChangeSetContext(context);

        if (StringUtil.trimToNull(changeLogFile) != null) {
            changeLogWriter.print(changeLogFile);
        } else {
            PrintStream outputStream = System.out;
            changeLogWriter.print(outputStream);
        }
    }

}
