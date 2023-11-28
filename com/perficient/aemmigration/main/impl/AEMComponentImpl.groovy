package com.perficient.aemmigration.main.impl

import com.perficient.aemmigration.main.AEMComponent

class AEMComponentImpl implements AEMComponent{
    String resourceType
    String markup
    String regexPattern = null
    String xmlMatcher = null
    Map properties = [:]

    AEMComponentImpl(String resourceType, String text, Map properties=[:], String regexPattern=null, String xmlMatcher=null){
        this.resourceType = resourceType
        this.markup = text
        this.regexPattern = regexPattern
        this.xmlMatcher = xmlMatcher
        this.properties = properties
    }

    String toString(){
        return this.resourceType + ":" + this.text
    }

    Map getProperties(){
        return properties
    }

    getProperty(String key){
        return properties[key] ?: null
    }

    String getResourceType() {
        return resourceType
    }

    void setResourceType(String resourceType) {
        this.resourceType = resourceType
    }

    String getMarkup() {
        return markup
    }

    void setMarkup(String markup) {
        this.markup = markup
    }

    String getRegexPattern() {
        return regexPattern
    }

    void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern
    }

    String getXmlMatcher() {
        return xmlMatcher
    }

    void setXmlMatcher(String xmlMatcher) {
        this.xmlMatcher = xmlMatcher
    }
}