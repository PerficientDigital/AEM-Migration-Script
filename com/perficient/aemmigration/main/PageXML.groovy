package com.perficient.aemmigration.main

import groovy.xml.slurpersupport.GPathResult

interface PageXML {
    String title
    String description
    String content
    GPathResult rawXML
    String getTitle()
    void setTitle(String title)
    String getDescription()
    void setDescription(String description)
    String getContent()
    void setContent(String content)
}
