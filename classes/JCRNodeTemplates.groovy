package classes

import groovy.xml.slurpersupport.GPathResult

class JCRNodeTemplates {

    static Map pageNode() {
        def p = [:]
        p['xmlns:cq']='http://www.day.com/jcr/cq/1.0'
        p['xmlns:jcr']='http://www.jcp.org/jcr/1.0'
        p['xmlns:nt']='http://www.jcp.org/jcr/nt/1.0'
        p['xmlns:sling']='http://sling.apache.org/jcr/sling/1.0'
        p['jcr:primaryType']='cq:Page'
        return p
    }

    static Map pageContentNode(PageMappingsCSV legacyPageData, PageXML pageXml, String pageTemplate , String pageResourceType){
            def pageProperties = [:]
            pageProperties['jcr:primaryType']='cq:PageContent'
            pageProperties['jcr:title'] = pageXml.getTitle()
            pageProperties['cq:contextHubPath'] = '/etc/cloudsettings/default/contexthub'
            pageProperties['cq:contextHubSegmentsPath'] = '/etc/segmentation/contexthub'
            pageProperties['cq:template'] = pageTemplate
            pageProperties['sling:resourceType'] = pageResourceType
            pageProperties['legacyUrl'] = legacyPageData.getLegacyUrl()
            pageProperties['migrationBatch'] = legacyPageData.getBatch()
            pageProperties['jcr:description'] = pageXml.getDescription()

            return pageProperties
    }

    static Map unstructuredNode() {
        def p = [:]
        p['jcr:primaryType']='nt:unstructured'
        return p
    }

    static componentNode(String resourceType, Map properties = [:]){
        def p = unstructuredNode()
        p['sling:resourceType']=resourceType
        properties.each{ k, v -> p[k] = v }
        return p
    }

    //todo: make this similar to above style.
    static String assetXml(){
       return '''<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:exif="http://ns.adobe.com/exif/1.0/" xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/" xmlns:tiff="http://ns.adobe.com/tiff/1.0/" xmlns:xmp="http://ns.adobe.com/xap/1.0/" xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/" xmlns:stEvt="http://ns.adobe.com/xap/1.0/sType/ResourceEvent#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dam="http://www.day.com/dam/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:mix="http://www.jcp.org/jcr/mix/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="dam:Asset">
    <jcr:content
        jcr:primaryType="dam:AssetContent">
        <metadata
            jcr:primaryType="nt:unstructured"/>
        <related jcr:primaryType="nt:unstructured"/>
    </jcr:content>
</jcr:root>
'''
    }

}
