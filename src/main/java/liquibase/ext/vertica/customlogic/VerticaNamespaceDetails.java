package liquibase.ext.vertica.customlogic;

import liquibase.parser.LiquibaseParser;
import liquibase.parser.NamespaceDetails;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.serializer.LiquibaseSerializer;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;

/**
 * Created by vesterma on 03/03/14.
 */

public class VerticaNamespaceDetails implements NamespaceDetails {

    public static final String VERTICA_NAMESPACE = "http://www.liquibase.org/xml/ns/dbchangelog-ext/vertica";

    public static final String VERTICA_XSD = VERTICA_NAMESPACE + "-latest.xsd";

    @Override
    public int getPriority() {
        return PRIORITY_EXTENSION;
    }

    @Override
    public boolean supports(LiquibaseSerializer serializer, String namespace) {
        if (namespaceCorrect(namespace) && serializer instanceof XMLChangeLogSerializer) {
            return true;
        }
        return false;
    }

    @Override
    public boolean supports(LiquibaseParser parser, String namespace) {
        if (namespaceCorrect(namespace) && parser instanceof XMLChangeLogSAXParser) {
            return true;
        }
        return false;
    }

    private boolean namespaceCorrect(String namespace) {
        return namespace.equals(VERTICA_NAMESPACE) || namespace.equals(VERTICA_XSD);
    }

    @Override
    public String getShortName(String namespace) {
        return "vertica";
    }

    @Override
    public String getSchemaUrl(String namespace) {
        return VERTICA_XSD;
    }

    @Override
    public String[] getNamespaces() {
        return new String[]{
                VERTICA_NAMESPACE
        };
    }
}
