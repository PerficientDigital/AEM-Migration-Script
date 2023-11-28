package com.perficient.aemmigration.templates;

import com.perficient.aemmigration.main.PageMappingsCSV;
import com.perficient.aemmigration.main.PageXML;
import groovy.xml.MarkupBuilder;

interface AEMTemplate {
    void renderPage(PageMappingsCSV pageMappingsCSV, PageXML pageXml, MarkupBuilder outXml, Map replacements);
}
