# Groovy AEM Migration Tool

Migration tool for migrating content into AEM by generating a Content Package.

## Quickstart

To use the script:

1. Checkout the project
2. Copy the `sample-file-mappings.csv` to `[configdir]/file-mappings.csv` and add any direct file mappings in source,target format
3. Copy the `sample-page-mappings.csv` to `[configdir]/page-mappings.csv` and add any page xml mappings in source,target format
4. Copy the `sample-filter.xml` to `filter.xml` and add the filter paths
5. Copy the `sample-properties.xml` to `[configdir]/properties.xml` and update the package name and group
6. Copy the `sample-replacement-config.json` to `[configdir]/replacement-config.json` and add / update the replacement configuration
7. Copy the `sample-replacements.csv` to `[configdir]/replacements.csv` and add any replacements
5. Copy the content from legacy content export to the `work/source` directory
6. Run the script with `groovy migrate.groovy [configdir] [batch]`

Use:

`groovy migrate.groovy [config] [batch (Optional)]`

The end result will be a Content Package ZIP file in the `work` directory.

## Script Parameters

There are only two parameters which can be provided when executing the script

 - **config** -- This required parameter is the first parameter to the script. It should be a directory containing the configuration files for the migration run. You can create multiple configuration directories to allow you keep a history of previous migration runs.
 - **batch** -- This optional parameter will only process pages with the specified Batch ID from the page-mappings.csv. This will also auto-generate a filter.xml based on these paths. 
 
## Configuration Files

The following files are used to control how the script is executed:

 - **file-mappings.csv** -- contains a list of files which should be copied along with the page content. Can also be used to populate a list of replacements within the migrated text.
 - **page-mappings.csv** -- contains a list of the pages which should be migrated. Additional columns can be added and will be available in the `pageData` parameter in the templates. Key attributes include:
    - **Status** -- must be the first column. Used to exclude pages within the list from being migrated, useful when using a larger content inventory spreadsheet to inform the migration process. The status of `Remove` or `Missing` will skip a page from being migrated.
    - **Batch** -- batches are used to control which pages can be included in a migration with the optional Batch script parameter, they are also useful for tracking when and from where content was migrated.
    - **Template** -- the template to load. The templates are located in the `templates` folder and are loaded by name. So the template "content" would be found at "templates/content.groovy"
    - **Legacy Url** -- the URL for the legacy page, this can be used to generate replacements or to populate a [Redirect Map Manager](https://adobe-consulting-services.github.io/acs-aem-commons/features/redirect-map-manager/index.html) configuration
    - **Source Path** -- the path to look for the source file under the `work/source` directory. Should start with a slash.
    - **New Path** -- the new AEM repository path for the page. 
    - **New Url** -- the migrated AEM URL. Should end in .html
 - **filter.xml** -- the filter.xml to be used if the batch parameter is not specified
 - **properties.xml** -- used to set the package version, group ID and name
 - **replacement-config.json** -- configures the generation of the replacement map. The minimal example shows replacing the old Paths and URLs with the new URL from the `page-mappings.csv` and `file-mappings.csv`. You can also also use regular expressions to extract the replacements by setting the mode to `regex` and setting the following parameters:
    - **extractionPattern** -- a regular expression to extract the desired value
    - **sourceReplacement** -- the text used for the replacement source, can use parameters from the `extractionPattern`
    - **targetReplacement** -- optional, though if not used a `targetKey` must be specified, the text used for the replacement target, can use parameters from the `extractionPattern`
 - **replacements.csv** -- a list of replacements to perform. Must be referenced in the `replacement-config.json` to use
 
## Templates

Each template is a groovy file with a single function:

    void renderPage(Map pageData, GPathResult inXml, MarkupBuilder outXml, Map replacements)

This function will be called by the migration script with:

 - **pageData** - a Map of data loaded from the row in the `page-mappings.csv`
 - **inXml** - The parsed source XML File
 - **outXml** - The .content.xml file to which to write the migrated content
 - **replacements** - a map of replacement strings
 
Additionally, a .commons.groovy is provided to handle a number of common AEM structures including components, the page metadata and performing replacements. See the `templates/content.groovy` template as an example.
