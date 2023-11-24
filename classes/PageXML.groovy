package classes

import groovy.xml.slurpersupport.GPathResult

class PageXML {
    String title
    String description
    String content
    GPathResult rawXML

    PageXML(GPathResult inXml){
       this.title = inXml.metadata.title.toString()
        if (this.title.isEmpty()) {
            this.title = inXml.title.toString()
        }

        this.description = inXml.metadata.description.toString()
        if (this.description.isEmpty())
            this.description = inXml.description.toString()

        this.content = inXml.content.toString()

        rawXML = inXml
    }

    String getTitle() {
        return title
    }

    void setTitle(String title) {
        this.title = title
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    String getContent() {
        return content
    }

    void setContent(String content) {
        this.content = content
    }
}
