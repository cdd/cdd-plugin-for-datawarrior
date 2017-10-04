DataWarrior Plugin-SDK
======================

This file describes the concept and all steps needed to create a DataWarrior plugin.
A plugin, if put in the plugin directory of a DataWarrior installation, adds a new menu item
to the DataWarrior 'Database' menu, which allows to retrieve data from an external database
or another data source to directly display it in a new DataWarrior document window.

Developing a plugin is easy for even an unexperienced Java developer. It does not require
any DataWarrior source code nor any knowledge about it. Everything that is needed is a
properly installed JDK and the files that come with the datawarriorPluginSDK.zip file.


Content
-------

readme.txt                           : this file
ExamplePluginTask1.java              : complete example plugin source code; used by 'buildAll'
ExamplePluginTask2.java              : example plugin code using structure editor; used by 'buildAll'
org/openmolecules/datawarrior/plugin : package containing two java interface files; used by 'buildAll'
tasknames                            : contains plugin task names; used by 'buildAll'
buildAll                             : short Linux/Mac script to build examplePlugin.jar from the source
examplePlugin.jar                    : plugin file that can be placed into the DataWarrior 'plugin' folder


Concept
-------

When DataWarrior (4.5.3 or higher) is launched, it processes all '*.jar' files
within the 'plugin' folder in the following way:
- Any plugin is supposed to contain a file called 'tasknames' on the top level. DataWarrior tries
  to open this file and extracts class names from it (full path name, one per line).
- These classes are assumed to implement the interface org.openmolecules.datawarrior.plugin.IPluginTask,
  which defines methods to name the task, to show a configuration dialog and to execute the task.
- DataWarrior tries to instantiate each task class and, if successful, adds a menu item for every task
  to the 'Database' menu.
- When a task's menu item is selected, the task is asked to show its configuration dialog.
- When the user clicks OK, the task is asked to convert the dialog settings into key-value pairs.
- The task is asked to validate the configuration, i.e. the key-value pairs.
- If valid, the task is asked to execute the configuration and is passed a call-back object.
- The call-back object provides methods to allocate a new document window and to populate its data model.


Example
-------

The OpenmoleculesExamplePluginTask.java contains the Java source code for three little examples
combined in one PluginTask:

- Simple:   Transfers a small 2D-String array into a new DataWarrior document.
            DataWarrior creates default views and filters.
- SMILES:   One source column contains chemical structures encoded as SMILES strings.
            The source table is transferred into a new DataWarrior document and a new column is created
            that contains chemical structures, which are automatically created from the SMILES codes.
            DataWarrior creates default views and filters.
- Molfiles: Transfers a small 2D-String array into a new DataWarrior document.
            An additional column is created that is populated with chemical structures, which are
            automatically produced from provided molfiles.
            Custom views with custom settings and custom filters are created by providing a template string.


Steps to create a new plugin
----------------------------

- Copy tasknames, buildAll, and the org folder to a new directory.
- Create a new SomeUniquePluginTask.java file and class implementing the IPluginTask interface.
- Study the comments and implement all methods of the interface. (See ExamplePluginTask?.java)
- For the implementation of the run() method study the method descriptions of the IPluginHelper interface.
- Ensure that 'tasknames' contains the correct class and, if used, package name(s)
- Correct plugin class name and jar-file name in the buildAll script
- Run buildAll and copy the created plugin jar file into the 'plugin' folder within the 'datawarrior'
  installation folder. Launch DataWarrior and look for the new menu item in the 'Database' menu.




