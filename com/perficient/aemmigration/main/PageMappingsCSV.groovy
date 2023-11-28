package com.perficient.aemmigration.main


//Status,Batch,Template,Legacy Url,Source Path,New Path,New Url
//add'l props accessed through rawCSV if updated
interface PageMappingsCSV {
    String status
    String batch
    String template
    String legacyUrl
    String sourcePath
    String newPath
    String newUrl

    def rawCSV = [:]

    Boolean inBatch(String batch)

    String getStatus()
    void setStatus(String status)
    String getBatch()
    void setBatch(String batch)
    String getTemplate()
    void setTemplate(String template)
    String getLegacyUrl()
    void setLegacyUrl(String legacyUrl)
    String getSourcePath()
    void setSourcePath(String sourcePath)
    String getNewPath()
    void setNewPath(String newPath)
    String getNewUrl()
    void setNewUrl(String newUrl)
    Boolean isInvalid()
    Boolean processPage(batch)
}
