package templates

import classes.JCRNodeTemplates
import classes.PageMappingsCSV
import classes.PageXML
import groovy.xml.MarkupBuilder


void renderPage(PageMappingsCSV pageMappingsCSV, PageXML pageXml, MarkupBuilder outXml, Map replacements){

    def pageTemplate = '/conf/sample/settings/wcm/templates/content-page'
    def pageResourceType = 'sample/components/structure/page'


    def pageProperties = JCRNodeTemplates.pageContentNode(pageMappingsCSV, pageXml ,pageResourceType,pageTemplate)
    
    outXml.'jcr:root'(JCRNodeTemplates.pageNode()) {
        'jcr:content'(pageProperties) {
            'root'(JCRNodeTemplates.componentNode('wcm/foundation/components/responsivegrid')){
                'responsivegrid'(JCRNodeTemplates.componentNode('wcm/foundation/components/responsivegrid')){
                    'title'(JCRNodeTemplates.componentNode('sample/components/content/title', ['fontSize': 'h1', 'header': pageXml.getTitle()]))
                    'text'(JCRNodeTemplates.componentNode('sample/components/content/text', ['textIsRich': true, 'text': pageXml.getContent()]))
                }
            }
        }
    }
}