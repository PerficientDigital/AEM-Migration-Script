package classes

class AEMComponent{
    def resourceType
    def text
    def regexPattern = null
    def properties = [:]

    AEMComponent(String resourceType, String text,Mapproperties=[:],String regexPattern=null){
        this.resourceType = resourceType
        this.text = text
        this.regexPattern = regexPattern
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

}