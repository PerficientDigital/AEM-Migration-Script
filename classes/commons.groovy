package classes

import groovy.xml.slurpersupport.GPathResult
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



String replace(String source, Map replacements){
    replacements.each{ k, v -> 
        if(source.contains(k)){
            source = source.replace(k,v)
        }
    }
    return source
}
