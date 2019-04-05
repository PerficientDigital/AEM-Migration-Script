#!/usr/bin/env groovy

@Grab('com.xlson.groovycsv:groovycsv:1.3')
import static com.xlson.groovycsv.CsvParser.parseCsv

import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.Field
import groovy.util.AntBuilder
import groovy.util.XmlSlurper

import java.nio.file.Files
import java.nio.file.StandardCopyOption

// configure settings here
@Field 
final SEPARATOR = ","
@Field 
final ENCODING = "UTF-8"

start = new Date()

if(args.length < 1) {
    println 'groovy migrate.groovy [configdir] [batch (Optional)]'
    System.exit(1)
}

batch = ""
if(args.length == 2) {
    batch = args[1]
    println "Using batch ${batch}"
}

Map loadReplacements() {
    def count = 0

    def cf = new File('work/config/replacement-config.json')
    assert cf.exists()
    def slurper = new JsonSlurper()
    def config = slurper.parseText(cf.text)

    def replacements = [:]

    config.each{file,cfgs->
        println "Processing replacements for ${file}..."

        def sf = new File('work/config/'+file)
        assert sf.exists()

        cfgs.each{cfg ->
            println "Processing configuration: ${cfg}"
            def source = parseCsv(sf.getText(ENCODING), separator: SEPARATOR)
            for (line in source) {
                if(cfg.mode == 'mapping'){
                    replacements[line[cfg.sourceKey]] = line[cfg.targetKey]
                } else {
                    def key = line[cfg.sourceKey]
                    if(cfg.targetKey){
                        replacements[key.replaceAll(cfg.extractionPattern, cfg.sourceReplacement)] = line[cfg.targetKey]
                    } else {
                        replacements[key.replaceAll(cfg.extractionPattern, cfg.sourceReplacement)] = key.replaceAll(cfg.extractionPattern, cfg.targetReplacement)
                    }
                }
                count++
            }
        }
    }

    println "Generated ${count} replacements in ${TimeCategory.minus(new Date(), start)}"
    
    return replacements
}

Map loadTemplates() {
    println 'Loading templates...'
    def count = 0
    def templates = [:]
    GroovyShell shell = new GroovyShell()
    new File('templates').eachFile (FileType.FILES) { template ->
        if(!template.getName().startsWith('.') && template.getName().endsWith('groovy')){
            def name = template.getName().replace('.groovy','')
            println "Loading template $template as $name"
            templates[name] = shell.parse(template)
            count++
        }
    }
    println "Loaded ${count} templates in ${TimeCategory.minus(new Date(), start)}"
    return templates
}

void processPages(File source, File jcrRoot) {
    
    def templates = loadTemplates()
    def replacements = loadReplacements()
    
    def pageFile = new File('work/config/page-mappings.csv')
    def count = 0
    def migrated = 0
    println 'Processing pages...'
    for (pageData in parseCsv(pageFile.getText(ENCODING), separator: SEPARATOR)) {

        println "Processing page ${count + 1}"

        def process = true
        if(batch?.trim() && !batch.equals(pageData['Batch'])){
            process = false
        }
        if('Remove' == pageData[0] || 'Missing' == pageData[0]) {
            process = false
        }
        if(process){

            def template = templates[pageData['Template']]
            assert template : "Template ${pageData['Template']} not found!"

            def sourceFile = new File(pageData['Source Path'],source)
            assert sourceFile.exists() : "Source file ${sourceFile} not found!"

            println "Using template ${pageData['Template']} for ${sourceFile}"

            def inXml = new XmlSlurper().parseText(sourceFile.getText(ENCODING))

            def writer = new StringWriter()
            def outXml = new MarkupBuilder(writer)

            println 'Rendering page...'
            template.renderPage(pageData, inXml, outXml, replacements)

            println 'Creating parent folder...'
            def targetFile = new File(pageData['New Path']+'/.content.xml',jcrRoot)
            targetFile.mkdirs()
            targetFile.delete()

            println "Writing results to $targetFile"
            targetFile.write(writer.toString(),ENCODING)
            migrated++
        } else {
            println 'No action required...'
        }

        count++
    }
    println "${count} pages processed and ${migrated} migrated in ${TimeCategory.minus(new Date(), start)}"
}

def configDir = new File(args[0])
assert configDir.exists()
println 'Copying configuration to work dir...'
def workConfig = new File('work/config')
if(!workConfig.exists()){
    workConfig.mkdirs()
}
configDir.eachFile (FileType.FILES) { file ->
    Files.copy(file.toPath(), new File(workConfig.getAbsolutePath()+'/'+file.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES)
}

def base = new File('work')
def source = new File('source',base)
println "Using source: ${source}"
def target = new File('target',base)
def jcrRoot = new File('jcr_root',target)
println "Using target: ${target}"

println 'Clearing target...'
target.deleteDir();
target.mkdir();

processPages(source,jcrRoot)

println 'Updating filter.xml...'
def vlt = new File('META-INF/vault',target)
vlt.mkdirs()
if(batch?.trim()){
    def writer = new StringWriter()
    def filterXml = new MarkupBuilder(writer)
    
    def pageFile = new File('work/config/page-mappings.csv')
    
    filterXml.'workspaceFilter'('version':'1.0'){
        for (pageData in parseCsv(pageFile.getText(ENCODING), separator: SEPARATOR)) {
            if(batch.equals(pageData['Batch']) && 'Remove' != pageData[0] && 'Missing' != pageData[0]){
                'filter'('root':pageData['New Path']){
                    'include'('pattern':pageData['New Path'])
                    'include'('pattern':"${pageData['New Path']}/jcr:content.*")
                }
            }
        }
    }
    new File('filter.xml',vlt) << writer.toString()
} else {
    def filter = new File('filter.xml',workConfig)
    assert filter.exists()
    Files.copy(filter.toPath(), new File('filter.xml',vlt).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES)
}

def today = new Date().format("yyyy-MM-dd")
println 'Updating properties.xml...'
def propertiesXml = new File('properties.xml',workConfig)
assert propertiesXml.exists()
new File('properties.xml',vlt) << propertiesXml.getText().replace('${version}',today)

println 'Creating package...'
def ant = new AntBuilder()
ant.zip(destfile: "${base.getAbsolutePath()}/migrated-content-${today}.zip", basedir: target)

println "Package saved to: work/migrated-content-${today}.zip"

println "Content migrated in ${TimeCategory.minus(new Date(), start)}"

println "Package created successfully!!!"
