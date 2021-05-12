package liquibase.ext.vertica.snapshot;

import liquibase.CatalogAndSchema;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.structure.Projection;
import liquibase.ext.vertica.structure.Segmentation;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.jvm.JdbcSnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.util.StringUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 27/11/13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 * TODO: need to change for the ResultSetCache to become public so we could fill in the metadata?!? need to look for workaround...
 */
public class ProjectionSnapshotGenerator extends JdbcSnapshotGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof VerticaDatabase)
            return PRIORITY_DATABASE;
        return PRIORITY_NONE;

    }

    public ProjectionSnapshotGenerator(){
//        super(Projection.class, new Class[]{Table.class});
        super(Projection.class, new Class[]{Schema.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {

        VerticaDatabase database = (VerticaDatabase) snapshot.getDatabase();
        Schema schema = example.getSchema();

        List<CachedRow> projectionMetadataRs = null;
        try {
            VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());
//            projectionMetadataRs = ((JdbcDatabaseSnapshot) snapshot).getMetaData().getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), example.getName(), new String[]{"VIEW"});
            projectionMetadataRs = (verticaDatabaseSnapshot).getMetaData().getProjectionDefinition(schema.getName(),example.getName());
            if (projectionMetadataRs.size() > 0) {
                CachedRow row = projectionMetadataRs.get(0);
                String rawSchemaName = StringUtil.trimToNull(row.getString("TABLE_SCHEM"));
                String rawProjectionName = row.getString("PROJ_NAME");

                Projection projection = new Projection();
                projection.setName(cleanNameFromDatabase(rawProjectionName, database));

                CatalogAndSchema schemaFromJdbcInfo = ((AbstractJdbcDatabase) database).getSchemaFromJdbcInfo("", rawSchemaName);
                projection.setSchema(new Schema(schemaFromJdbcInfo.getCatalogName(), schemaFromJdbcInfo.getSchemaName()));
                projection.setAnchorTable((String) row.get("TABLE_NAME"));
                projection.setSubquery("Select * from " + (String) row.get("TABLE_NAME"));
                projection.setKSafe((String) row.get("K_SAFE"));
                projection.setOffset((Long) row.get("OFFSET"));
//                schema.addDatabaseObject(pr);
//                table.getAttribute("projections", List.class).add(pr);

                projection.setIsSegmented((Boolean) row.get("IS_SEGMENTED"));
                projection.setSegmentedBy((String) row.get("SEGMENT_EXRESSION"));
//                String segmentedby = ((VerticaDatabase)database).executeSQL("select export_objects('','"+schema.getName()+"."+projection.getAnchorTable()+"',false)");
                //if (segmentation != null){

//                if (projection.getIsSegmented()){
//                    //String segmentation =  (String) projection.get("SEGMENTATION");
//
//                    String pat = "SEGMENTED BY (.*)\\((.*)\\) (.*) (KSAFE|;)";
//                    Pattern pattern = Pattern.compile(pat);
//                    Matcher matcher = pattern.matcher(segmentedby);
//                    if (matcher.find()) {
//                        projection.setSegmentedBy(matcher.group(1)+"("+matcher.group(2)+")");
//                        projection.setNodes(matcher.group(3));
//
////                                pr.setAttribute("segmentedby",matcher.group(1)+"("+matcher.group(2)+")");
//
//                    } else {
//                        System.out.println("failed to parse segmentation: "+segmentedby);
//                    }
//
//          /*      try {
//                    projection.setDefinition(database.getProjectionDefinition(schemaFromJdbcInfo, projection.getName()));
//                } catch (DatabaseException e) {
//                    throw new DatabaseException("Error getting " + database.getConnection().getURL() + " projection with " + new GetProjectionDefinitionStatement(projection.getSchema().getCatalogName(), projection.getSchema().getName(), rawProjectionName), e);
//                }*/
//                }else{
//                    String pat = "UNSEGMENTED (.*)( KSAFE|;)";
//                    Pattern pattern = Pattern.compile(pat);
//                    Matcher matcher = pattern.matcher(segmentedby);
//                    if (matcher.find()) {
//                        projection.setSegmentedBy(null);
//                        projection.setNodes(matcher.group(1));
//
////                                pr.setAttribute("segmentedby",matcher.group(1)+"("+matcher.group(2)+")");
//
//                    } else {
//                        System.out.println("failed to parse segmentation: "+segmentedby);
//                    }
//                }

                return projection;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Projection.class)) {
            return;
        }

        if (foundObject instanceof Schema) {
//        if (foundObject instanceof Table) {
//            Table table = (Table) foundObject;
            Database database = snapshot.getDatabase();
            Schema schema = (Schema) foundObject;
//            Schema schema;
//            schema = table.getSchema();

//            table.setAttribute("projections",new ArrayList<Projection>());

            List<CachedRow> metadata = null;
            try {
                VerticaDatabaseSnapshot verticaDatabaseSnapshot = new VerticaDatabaseSnapshot(new DatabaseObject[0],snapshot.getDatabase(),snapshot.getSnapshotControl());
                metadata = (verticaDatabaseSnapshot.getMetaData().getProjectionDefinition(schema.getName(),null)); //, table.getName()));

                for (CachedRow projection : metadata) {
                    Projection pr = new Projection();
                    pr.setName(cleanNameFromDatabase((String) projection.get("PROJ_NAME"), database));
                    pr.setAnchorTable((String) projection.get("TABLE_NAME"));
                    pr.setSchema(schema);
                    pr.setSubquery("Select * from " + (String) projection.get("TABLE_NAME"));
                    pr.setKSafe((String) projection.get("K_SAFE"));
                    pr.setOffset((Long) projection.get("OFFSET"));
//                schema.addDatabaseObject(pr);
//                table.getAttribute("projections", List.class).add(pr);

                    pr.setIsSegmented((Boolean) projection.get("IS_SEGMENTED"));
                    pr.setSegmentedBy((String) projection.get("SEGMENT_EXRESSION"));
//                    if (pr.getIsSegmented()){
//                        //String segmentation =  (String) projection.get("SEGMENTATION");
//                        String segmentedby = ((VerticaDatabase)database).executeSQL("select export_objects('','"+schema.getName()+"."+pr.getAnchorTable()+"',false)");
//                        //if (segmentation != null){
//                            Segmentation segment = new Segmentation();
//                            String pat = "SEGMENTED BY (.*)\\((.*)\\) (.*) (KSAFE|;)";
//                            Pattern pattern = Pattern.compile(pat);
//                            Matcher matcher = pattern.matcher(segmentedby);
//                            if (matcher.find()) {
//                                segment.setExpression(matcher.group(1)+"("+matcher.group(2)+")");
//                                pr.setSegmentedBy(matcher.group(1)+"("+matcher.group(2)+")");
//                                pr.setNodes(matcher.group(3));
//
////                                pr.setAttribute("segmentedby",matcher.group(1)+"("+matcher.group(2)+")");
//
//                            } else {
//                                System.out.println("failed to parse segmentation: "+segmentedby);
//                            }
//                        //}
//                    }


                    schema.addDatabaseObject(pr);

                }

            } catch (SQLException e) {
                throw new DatabaseException(e);
            }


        }
    }
}
