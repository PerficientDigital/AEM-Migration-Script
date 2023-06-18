#!/usr/bin/env groovy
/**
*  Migration script for migrating legacy content using an AEM Content Package
*  See: https://github.com/PerficientDigital/AEM-Migration-Script
*/
@Grab('com.xlson.groovycsv:groovycsv:1.3')
import static com.xlson.groovycsv.CsvParser.parseCsv

@Grab('org.apache.tika:tika-core:1.14')
import org.apache.tika.Tika;

import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.Field
import groovy.ant.AntBuilder
import groovy.xml.XmlSlurper

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

    def cf = new File("work${File.separator}config${File.separator}replacement-config.json")
    assert cf.exists()
    def slurper = new JsonSlurper()
    def config = slurper.parseText(cf.text)

    def replacements = [:]

    config.each{file,cfgs->
        println "Processing replacements for ${file}..."

        def sf = new File("work${File.separator}config${File.separator}${file}")
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
    
    def pageFile = new File("work${File.separator}config${File.separator}page-mappings.csv")
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

            def sourceFile = new File(source, pageData['Source Path'])
            assert sourceFile.exists() : "Source file ${sourceFile} not found!"

            println "Using template ${pageData['Template']} for ${sourceFile}"

            def inXml = new XmlSlurper().parseText(sourceFile.getText(ENCODING))

            def writer = new StringWriter()
            def outXml = new MarkupBuilder(writer)

            println 'Rendering page...'
            template.renderPage(pageData, inXml, outXml, replacements)

            println 'Creating parent folder...'
            def targetFile = new File(jcrRoot.toString(), "${pageData['New Path'].toString()}${File.separator}.content.xml".toString())
            targetFile.parentFile.mkdirs()

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

def processFiles(sourceDir, targetDir) {
    def sourcePath = sourceDir.toPath()
    def targetPath = targetDir.toPath()

    Files.walk(sourcePath).forEach { sourceFile ->
        def targetFile = targetPath.resolve(sourcePath.relativize(sourceFile))

        if (Files.isDirectory(sourceFile)) {
            Files.createDirectories(targetFile)
        } else {
            Files.createDirectories(targetFile.getParent())
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        println "Copying ${sourceFile} to ${targetFile}"
    }
}

void processFile(File sourceFile, File assetRoot, Tika tika) {
    if (sourceFile.isDirectory()) {
        def targetDir = new File(assetRoot, sourceFile.name)
        targetDir.mkdirs()

        for (file in sourceFile.listFiles()) {
            def targetFile = new File(targetDir, file.name)
            processFile(file, targetFile, tika)
        }
    } else {
        // Process the file
        def mimeType = "application/octet-stream" // Default MIME type for directories

        println "Processing Source: ${sourceFile} Target: ${assetRoot}"

        if (sourceFile.isFile()) {
            try {
                mimeType = tika.detect(sourceFile)
            } catch (FileNotFoundException e) {
                // Handle file not found exception
                println "File not found: ${sourceFile}"
                return
            }
        }

        println 'Creating original.dir XML...'
        def writer = new StringWriter()
        def originalDirXml = new MarkupBuilder(writer)
        originalDirXml.'jcr:root'('xmlns:jcr':'http://www.jcp.org/jcr/1.0','xmlns:nt':'http://www.jcp.org/jcr/nt/1.0','jcr:primaryType':'nt:file'){
            'jcr:content'('jcr:mimeType': mimeType, 'jcr:primaryType': 'nt:resource')
        }
        def originalDirPath = assetRoot.getAbsolutePath() + File.separator + "_jcr_content" + File.separator + "renditions" + File.separator + "original.dir"
        def originalDir = new File(originalDirPath)
        originalDir.mkdirs()
        def contentXmlFile = new File(originalDir, ".content.xml")
        println "Writing results to $contentXmlFile"
        contentXmlFile.write(writer.toString(), ENCODING)

        println 'Copying original file...'
        def targetFile = new File(assetRoot, sourceFile.name)
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

def newConfigDir = new File(args[0])
assert newConfigDir.exists()
println 'Copying configuration to work dir...'
def workConfigDir = new File("work${File.separator}config")
if (!workConfigDir.exists()) {
    workConfigDir.mkdirs()
}
newConfigDir.eachFile { file ->
    def destFile = new File(workConfigDir.getAbsolutePath() + File.separator + file.name)
    def inputFile = new File(file.absolutePath)
    def outputFile = new File(destFile.absolutePath)

    outputFile.parentFile.mkdirs()

    inputFile.withInputStream { inputStream ->
        outputFile.withOutputStream { outputStream ->
            outputStream << inputStream
        }
    }
}


def base = new File('work')
def source = new File(base,'source')
println "Using source: ${source}"
def target = new File(base,'target')
def jcrRoot = new File(target, 'jcr_root')
println "Using target: ${target}"

println 'Clearing target...'
target.deleteDir();
target.mkdir();

processPages(source,jcrRoot)
processFiles(new File("work${File.separator}source"), new File("work${File.separator}target${File.separator}jcr_root${File.separator}content${File.separator}dam"))

println 'Updating filter.xml...'
def vlt = new File("work/target/META-INF/vault")
vlt.mkdirs()
if (batch?.trim()) {
    def writer = new StringWriter()
    def filterXml = new MarkupBuilder(writer)

    def pageFile = new File("work/config/page-mappings.csv")

    filterXml.workspaceFilter(version: '1.0') {
        for (pageData in parseCsv(pageFile.getText(ENCODING), separator: SEPARATOR)) {
            if ('Remove' != pageData[0] && 'Missing' != pageData[0]) {
                filterXml.filter(root: pageData['New Path']) {
                    include(pattern: pageData['New Path'])
                    include(pattern: "${pageData['New Path']}/jcr:content.*")
                }
            }
        }
        for (fileData in parseCsv(new File("work/config/file-mappings.csv").getText(ENCODING), separator: SEPARATOR)) {
            if ('Remove' != fileData[0] && 'Missing' != fileData[0]) {
                filterXml.filter(root: fileData['Target']) {
                    include(pattern: fileData['Target'])
                    include(pattern: "${fileData['Target']}/jcr:content.*")
                }
            }
        }
    }
    new File(vlt, 'filter.xml').withWriter { fileWriter ->
        fileWriter << writer.toString()
    }
} else {
    def filter = new File('work/config/filter.xml')
    assert filter.exists()
    Files.copy(filter.toPath(), new File(vlt, 'filter.xml').toPath(), StandardCopyOption.REPLACE_EXISTING)
}

def now = new Date().format("yyyy-MM-dd-HH-mm-ss")
println 'Updating properties.xml...'
def propertiesXml = new File(workConfigDir, 'properties.xml')
assert propertiesXml.exists()
new File(vlt, 'properties.xml') << propertiesXml.getText().replace('${version}',now).replace('${name}',"migrated-content-${workConfigDir.getName()}")

println 'Creating package...'
def ant = new AntBuilder()
ant.zip(destfile: "${base.getAbsolutePath()}${File.separator}migrated-content-${workConfigDir.getName()}-${now}.zip", basedir: target)

println "Package saved to: work${File.separator}migrated-content-${workConfigDir.getName()}-${now}.zip"

println "Content migrated in ${TimeCategory.minus(new Date(), start)}"

println "Package created successfully!!!"