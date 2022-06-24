package liquibase.ext.vertica.change;

import liquibase.change.*;
import liquibase.database.Database;
import liquibase.ext.vertica.customlogic.VerticaNamespaceDetails;
import liquibase.ext.vertica.database.VerticaDatabase;
import liquibase.ext.vertica.statement.CreateProjectionStatement;
import liquibase.ext.vertica.structure.GroupedColumns;
import liquibase.ext.vertica.structure.Segmentation;
import liquibase.statement.SqlStatement;
import liquibase.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vesterma
 * Date: 11/11/13
 * Time: 13:00
 * To change this template use File | Settings | File Templates.
 */
/**
 * Creates a new projection.
 */
@DatabaseChange(name="createProjection", description = "create projections", priority = ChangeMetaData.PRIORITY_DATABASE)
public class CreateProjectionChange extends AbstractChange implements ChangeWithColumns<ColumnConfigVertica> {

    private String tableName;
    private String projectionName;
    private String schemaName;
    private String viewName;
    private String columnAliases;
    private String objectType;
    private Boolean reducedPrecision;
    private Boolean usingIndex;
    private String tableSpace;
    private Boolean forUpdate;
    private String orderby;
    private String    ksafe;
    private List<GroupedColumns> groupedColumns;
    private Segmentation segmentation;
    private String segmentedby;
    private Integer offset;
    private String nodes;
    private String subquery;

    private List<ColumnConfigVertica> columns;
    private String remarks;


    public String getColumnAliases() {
        return columnAliases;
    }

    public void setColumnAliases(String columnAliases) {
        this.columnAliases = columnAliases;
    }


    public CreateProjectionChange() {
        super();
        columns = new ArrayList<ColumnConfigVertica>();
        groupedColumns = new ArrayList<GroupedColumns>();

    }

    public String getProjectionName() {
        return projectionName;
    }



    public String getSchemaName() {
        return schemaName;
    }

    public String getOrderby() {
        return orderby;
    }

    public void setOrderby(String orderby) {
        this.orderby = orderby;
    }

    public void setProjectionName(String projectionName) {
        this.projectionName = projectionName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = StringUtil.trimToNull(schemaName);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void addColumn(ColumnConfigVertica column) {
        columns.add(column);
    }

    public List<ColumnConfigVertica> getColumns() {
        return columns;
    }


    @Override
    public void setColumns(List columns) {
        this.columns = columns;
    }

    public String getKsafe() {
        return ksafe;
    }

    public void setKsafe(String ksafe) {
        this.ksafe = ksafe;
    }

    public void removeColumn(ColumnConfig column) {
        columns.remove(column);
    }

    public String getSubquery() {
        return subquery;
    }

    public void setSubquery(String subquery) {
        this.subquery = subquery;
    }

    /*public Segmentation getSegmentation() {
        Segmentation seg = new Segmentation();
        seg.setNodes(nodes);
        seg.setExpression(segmentedby);
        seg.setUnsegmented(segmentedby==null  ? true : false);
        seg.setAllNodes(nodes==null || nodes.contains("ALL") ? true : false);
        seg.setOffset(offset);
        return seg;
    }

    public void setSegmentation(Segmentation segmentation) {
        this.segmentation = segmentation;
    }
*/
    public SqlStatement[] generateStatements(Database database) {

        CreateProjectionStatement statement = new CreateProjectionStatement(getSchemaName(),getProjectionName(),getTableName(),getColumns());
        statement.setGroupedColumns(getGroupedColumns());
        statement.setSchemaName(getSchemaName());

        statement.setOffset(getOffset());
        statement.setNodes(getNodes());
        statement.setSegmentedby(getSegmentedby());
//        statement.setSegmentation(getSegmentation());
        statement.setSubquery(getSubquery());
        statement.setOrderby(getOrderby());
        statement.setKsafe(getKsafe());
//        statement.setColumnAliases(getColumnAliases());
//        statement.setObjectType(getObjectType());
//        statement.setReducedPrecision(getReducedPrecision());
//        statement.setUsingIndex(getUsingIndex());
//        statement.setTableSpace(getTableSpace());
//        statement.setForUpdate(getForUpdate());
//        statement.setQueryRewrite(getQueryRewrite());


        return new SqlStatement[]{statement};
    }

    public String getConfirmationMessage() {
        /*List<String> names = new ArrayList<String>(columns.size());
        for (ColumnConfig col : columns) {
            names.add(col.getName() + "(" + col.getType() + ")");
        }

        return "Columns " + StringUtil.join(names, ",") + " of " + getTableName() + " modified";*/
        return "Projection " + getProjectionName() + " has been created";
    }

    @Override
    protected Change[] createInverses() {
        DropProjectionChange inverse = new DropProjectionChange();
        inverse.setSchemaName(getSchemaName());
        inverse.setTableName(getTableName());
        inverse.setProjectionName(getProjectionName());
        inverse.setCascade(Boolean.FALSE);

        return new Change[]{
                inverse
        };
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

    public GroupedColumns createGrouped(){
        GroupedColumns group = new GroupedColumns();
        groupedColumns.add(group);
        return group;
    }

    public Segmentation createSegmentation(){
        segmentation = new Segmentation();
        return segmentation;
    }

    public List<GroupedColumns> getGroupedColumns() {
        return groupedColumns;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSegmentedby() {
        return segmentedby;
    }

    public void setSegmentedby(String segmentedby) {
        this.segmentedby = segmentedby;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getSerializedObjectNamespace() {
        return VerticaNamespaceDetails.VERTICA_NAMESPACE;

    }


}
