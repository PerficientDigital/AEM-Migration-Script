import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder


Map component(String resourceType) {
    return component(resourceType, [:])
}

Map component(String resourceType, Map properties) {
    def p = unstructured()
    p['sling:resourceType']=resourceType
    properties.each{ k, v -> p[k] = v }
    return p
}

Map pageProperties(Object pageData, GPathResult inXml, String template){
    pageProperties(pageData, inXml, template, null)
}

Map pageProperties(Object pageData, GPathResult inXml, String template, Map replacements){
    def pageProperties = [:]
    pageProperties['jcr:primaryType']='cq:PageContent'
    pageProperties['jcr:title'] = inXml.metadata.title.toString()
    pageProperties['cq:contextHubPath'] = '/etc/cloudsettings/default/contexthub'
    pageProperties['cq:contextHubSegmentsPath'] = '/etc/segmentation/contexthub'
    pageProperties['cq:template'] = template
    pageProperties['sling:resourceType'] = 'sample/components/structure/page'
    pageProperties['legacyPath'] = pageData['Source Path']
    pageProperties['migrationBatch'] = pageData['Batch']
    
    def description = inXml.metadata.description.toString()
    if(description?.trim()) {
        pageProperties['jcr:description'] = description
    }
    return pageProperties
}

String replace(String source, Map replacements){
    replacements.each{ k, v -> 
        if(source.contains(k)){
            source = source.replace(k,v)
        }
    }
    return source
}

Map rootProperties() {
    def p = [:]
    p['xmlns:cq']='http://www.day.com/jcr/cq/1.0'
    p['xmlns:jcr']='http://www.jcp.org/jcr/1.0'
    p['xmlns:nt']='http://www.jcp.org/jcr/nt/1.0'
    p['xmlns:sling']='http://sling.apache.org/jcr/sling/1.0'
    p['jcr:primaryType']='cq:Page'
    return p
}

Map unstructured() {
    def p = [:]
    p['jcr:primaryType']='nt:unstructured'
    return p
}