# Groovy AEM Content Import

This project supports migrating content to AEM.

To use the script:

1. Checkout the project
2. Copy the `sample-file-mappings.csv` to `[configdir]file-mappings.csv` and add any direct file mappings in source,target format
3. Copy the `sample-page-mappings.csv` to `[configdir]page-mappings.csv` and add any page xml mappings in source,target format
4. Copy the `sample-filter.xml` to `filter.xml` and add the filter paths
5. Copy the content from RedDot to the `work/source` directory
6. Run the script with `groovy migrate.groovy [configdir] [batch]`

Use:

`groovy migrate.groovy [config] [batch]`