## CDD Vault® Plugin for DataWarrior

CDD Vault® is now connected directly to DataWarrior. This plugin allows you to easily pull chemical and biological data into DataWarrior via CDD Vault® saved searches.  With your data safely and securely stored within CDD Vault, you can take advantage of the wide range of cheminformatics visualization and analysis features in DataWarrior. This plugin was originally authored by Thomas Sander.

### Installation
1. [Install DataWarrior](http://www.openmolecules.org/datawarrior/download.html)
2. Download CDDVaultDataWarriorPlugin.zip from the bottom of [this page](https://support.collaborativedrug.com/hc/en-us/articles/115005682303-API-via-DataWarrior-the-basics) and extract cddVaultTask.jar or run buildAll to create cddVaultTask.jar
3. Move cddVaultTask.jar to the 'plugin' folder within DataWarrior. *This folder should be a top-level folder in the DataWarrior program folder and may need to be created manually.*

### Usage
[Click here for detailed instructions.](https://support.collaborativedrug.com/hc/en-us/articles/115005682303-API-via-DataWarrior-the-basics)

### Notes
This plugin requires a CDD Vault® API key to function.  You can have the plugin store your API key, in which case the key is stored unencrypted. This option should never be selected when running DataWarrior on a shared machine.

Once a Search and at least one Project and/or Public Dataset is selected, click OK to populate your data into DataWarrior.  This may take some time to complete depending on the size of the results.

### Contents

- readme.md                               : this file
- src/CDDVaultTask.java                    : the CDD Vault® plugin for DataWarrior
- src/info/clearthought/layout             : Clearthought layout partial package (see for different license)
- src/org/openmolecules/datawarrior/plugin : DataWarrior SDK (see for different license)
- src/org/json                             : JSON package (see for different license)
- tasknames                                : contains plugin task names
- buildAll                                 : short Linux/Mac script to build Java class files and cddVaultTask.jar from the source
- runTest                                  : short Linux/Mac script to test all tasks without the DataWarrior application
