package com.perficient.aemmigration.main

interface AEMComponent{
    def resourceType
    def markup
    def regexPattern = null
    def xmlMatcher = null
    def properties = [:]

    Map getProperties()
    Object getProperty(String key);
    String getMarkup()
    String getResourceType()
    String getRegexPattern()
    String getXmlMatcher()
}