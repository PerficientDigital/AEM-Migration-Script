package classes
//Status,Batch,Template,Legacy Url,Source Path,New Path,New Url
//add'l props accessed through rawCSV if updated
class PageMappingsCSV {
    String status
    String batch
    String template
    String legacyUrl
    String sourcePath
    String newPath
    String newUrl

    Map rawCSV = [:]

    PageMappingsCSV(Map pageCSV){
        this.rawCSV = pageCSV
        this.status = pageCSV['Status']
        this.batch = pageCSV['Batch']
        this.template = pageCSV['Template']
        this.legacyUrl = pageCSV['Legacy Url']
        this.sourcePath = pageCSV['Source Path']
        this.newPath = pageCSV['New Path']
        this.newUrl = pageCSV['New Url']
    }

    Boolean inBatch(String batch){
        batch = batch?.trim()
        return batch ? batch == getBatch() : true
    }

    String getStatus() {
        return status
    }

    void setStatus(String status) {
        this.status = status
    }

    String getBatch() {
        return batch
    }

    void setBatch(String batch) {
        this.batch = batch
    }

    String getTemplate() {
        return template
    }

    void setTemplate(String template) {
        this.template = template
    }

    String getLegacyUrl() {
        return legacyUrl
    }

    void setLegacyUrl(String legacyUrl) {
        this.legacyUrl = legacyUrl
    }

    String getSourcePath() {
        return sourcePath
    }

    void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath
    }

    String getNewPath() {
        return newPath
    }

    void setNewPath(String newPath) {
        this.newPath = newPath
    }

    String getNewUrl() {
        return newUrl
    }

    void setNewUrl(String newUrl) {
        this.newUrl = newUrl
    }

    Boolean isInvalid(){
        return getRawCSV()[0] == "Missing" || getRawCSV()[0]=="Remove"
    }

    Boolean processPage(batch){
        Boolean doProcess = inBatch(batch)
        return inBatch(batch) && !isInvalid()
    }
}
