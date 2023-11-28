package templates;

import classes.PageMappingsCSV;
import classes.PageXML;
import groovy.xml.MarkupBuilder;
import java.util.Map;

public interface AEMTemplate {
    void renderPage(PageMappingsCSV pageMappingsCSV, PageXML pageXml, MarkupBuilder outXml, Map replacements);
}
