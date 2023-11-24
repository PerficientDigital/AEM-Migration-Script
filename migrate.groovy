#!/usr/bin/env groovy
import classes.JCRNodeTemplates
import classes.PageMappingsCSV
import classes.PageXML

/*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

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

void processPages(File source, File jcrRoot, String batch = "") {
    
    def templates = loadTemplates()
    def replacements = loadReplacements()
    
    def pageFile = new File("work${File.separator}config${File.separator}page-mappings.csv")
    def count = 0
    def migrated = 0
    println 'Processing pages...'
    for (row in parseCsv(pageFile.getText(ENCODING), separator: SEPARATOR)) {
        PageMappingsCSV pageMapping = new PageMappingsCSV(row)

        println "Processing page ${count + 1}"

        if( pageMapping.processPage(batch) ){
            def template = templates[pageMapping.getTemplate()]
            assert template : "Template ${pageMapping.getTemplate()} not found!"

            def sourceFile = new File(pageMapping.getSourcePath(),source)
            assert sourceFile.exists() : "Source file ${sourceFile} not found!"

            println "Using template ${pageMapping.getTemplate()} for ${sourceFile}"

            def pageXml = new PageXML(new XmlSlurper().parseText(sourceFile.getText(ENCODING)))

            def writer = new StringWriter()
            def outXml = new MarkupBuilder(writer)

            println 'Rendering page...'
            template.renderPage(pageMapping, pageXml, outXml, replacements)

            println 'Creating parent folder...'
            def targetFile = new File("${pageMapping.getNewPath()}${File.separator}.content.xml",jcrRoot)
            targetFile.getParentFile().mkdirs()

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

void processAssets(File source, File jcrRoot){
    
    def assetXml = JCRNodeTemplates.assetXml()
    def tika = new Tika()
    def files = new File("work${File.separator}config${File.separator}file-mappings.csv")
    def count = 0
    def migrated = 0
    println 'Processing assets...'
    //expect csv to just have batch, source, and target
    for (fileData in parseCsv(files.getText(ENCODING), separator: SEPARATOR)) {
        def assetRoot = new File(fileData['Target'], jcrRoot)
        def sourceFile = new File(fileData['Source'], source)
        def mimeType = tika.detect(sourceFile)
        
        println "Processing Source: ${sourceFile} Target: ${assetRoot}"
        
        println 'Creating original.dir XML...'
        def writer = new StringWriter()
        def originalDirXml = new MarkupBuilder(writer)
        originalDirXml.'jcr:root'('xmlns:jcr':'http://www.jcp.org/jcr/1.0','xmlns:nt':'http://www.jcp.org/jcr/nt/1.0','jcr:primaryType':'nt:file'){
            'jcr:content'('jcr:mimeType': mimeType, 'jcr:primaryType': 'nt:resource')
        }
        def originalDir = new File("_jcr_content${File.separator}renditions${File.separator}original.dir${File.separator}.content.xml",assetRoot)
        originalDir.getParentFile().mkdirs()
        originalDir.newWriter().withWriter { w ->
            w << writer.toString()
        }
        
        println 'Copying original asset...'
        Files.copy(sourceFile.toPath(), new File("_jcr_content${File.separator}renditions${File.separator}original",assetRoot).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES)
        
        println 'Writing asset .content.xml...'
        new File('.content.xml',assetRoot).newWriter().withWriter { w ->
            w << assetXml
        }
    }
}

void generateFilter(batch) {
    println 'Updating filter.xml...'
    def vlt = new File("META-INF${File.separator}vault", target)
    vlt.mkdirs()
    if (batch?.trim()) {
        def writer = new StringWriter()
        def filterXml = new MarkupBuilder(writer)

        def pageFile = new File("work${File.separator}config${File.separator}page-mappings.csv")

        filterXml.'workspaceFilter'('version': '1.0') {
            for (pageData in parseCsv(pageFile.getText(ENCODING), separator: SEPARATOR)) {
                PageMappingsCSV pageMapping = new PageMappingsCSV(pageData)
                if (pageMapping.processPage(batch)) {
                    'filter'('root': pageMapping.getNewPath()) {
                        'include'('pattern': pageMapping.getNewPath())
                        'include'('pattern': "${pageMapping.getNewPath()}/jcr:content.*")
                    }
                }
            }
            for (fileData in parseCsv(new File("work${File.separator}config${File.separator}file-mappings.csv").getText(ENCODING), separator: SEPARATOR)) {
                if (batch.equals(fileData['Batch']) && 'Remove' != fileData[0] && 'Missing' != fileData[0]) {
                    'filter'('root': fileData['Target']) {
                        'include'('pattern': fileData['Target'])
                        'include'('pattern': "${fileData['Target']}/jcr:content.*")
                    }
                }
            }
        }
        new File('filter.xml', vlt) << writer.toString()
    } else {
        def filter = new File('filter.xml', workConfig)
        assert filter.exists()
        Files.copy(filter.toPath(), new File('filter.xml', vlt).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    }
}

void generatePropertiesXml(File workFolder, String createTime) {
    println 'Updating properties.xml...'
    def propertiesXml = new File('properties.xml', workFolder)
    assert propertiesXml.exists()
    new File('properties.xml', vlt) << propertiesXml.getText().replace('${version}', createTime).replace('${name}', "migrated-content-${configDir.getName()}")
}



void main(args) {
    def start = new Date()
    String createTime = start.format("yyyy-MM-dd-HH-mm-ss")

    if(args.length < 1) {
        println 'groovy migrate.groovy [configdir] [batch (Optional)]'
        System.exit(1)
    }

    String batch = ""

    if(args.length == 2) {
        batch = args[1]
        println "Using batch ${batch}"
    }

    //initiate all files
    def configDir = new File(args[0])
    assert configDir.exists()
    def base = new File('work')
    def source = new File('source', base)
    def target = new File('target', base)
    def jcrRoot = new File('jcr_root', target)
    def workConfig = new File("work${File.separator}config")
    if(!workConfig.exists()){
        workConfig.mkdirs()
    }

    println "Using source: ${source}"
    println "Using target: ${target}"
    println 'Copying configuration to work dir...'



    configDir.eachFile (FileType.FILES) { file ->
        Files.copy(file.toPath(), new File(workConfig.getAbsolutePath()+File.separator+file.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES)
    }

    println 'Clearing target...'
    target.deleteDir();
    target.mkdir();

    processPages(source, jcrRoot, batch)
    processAssets(source, jcrRoot)
    generateFilter(batch)
    generatePropertiesXml(workConfig,createTime)

    println 'Creating package...'
    def ant = new AntBuilder()
    ant.zip(destfile: "${base.getAbsolutePath()}${File.separator}migrated-content-${configDir.getName()}-${createTime}.zip", basedir: target)
    println "Package saved to: work${File.separator}migrated-content-${configDir.getName()}-${createTime}.zip"
    println "Content migrated in ${TimeCategory.minus(new Date(), start)}"
    println "Package created successfully!!!"
}

main(args)