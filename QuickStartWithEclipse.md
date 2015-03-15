# Introduction #

If you have the development environment Eclipse installed (available from http://Eclipse.org, the following steps may get you started quickly.

# Steps #

Download the Scriptic All-in-1 zip archive from http://code.google.com/p/scriptic/downloads/detail?name=scriptic-all-in-one.zip

Unpack the zip archive in your Eclipse _`workspace`_ folder, yielding there a folder hierarchy
```
  scriptic/
    dimensions
    doc
    Examples
    lib
```

Tell the Eclipse text editor that Scriptic source files are like Java source files, etc:
  * In Eclipse, issue the menu command _`Window|Preferences`_, which makes the **Preferences** dialog appear
  * Select _`General|Workspace`_, and switch on the check box _`Refresh automatically`_
  * Select _`General|Editors|File Associations`_.
    * In the _`File Associations`_ panel, click the link _`Content Types`_.
      * In the _`Content Types`_ panel, open the map _`Text`_; click _`Java Source File`_.
  * Add file association for file type _`*.sawa`_.
  * Select _`General|Editors|Text Editors`_.
    * Switch on the check box _`Insert spaces for tabs`_

In Eclipse, issue the menu command _`File|New|Project`_ to create a new development project using a wizard.
  * in the first wizard page, select _`Java project`_, and go to the next page
  * in the second wizard page, name the new project, for instance _`Scriptic Demo`_, and go to the next page
  * in the third page, _`Java Settings`_:
    * in tab _`Source`_, select the displayed folder _`src`_
      * click the link _`Configure inclusion and exclusion filters`_
      * in the `Inclusion and Exclusion Patterns` dialog, enter _**exclusion**_ pattern _`**/*.sawa`_, and click _`Finish`_ to close the dialog
    * in tab _`Libraries`_, click button _`Add External JARs...`_
      * using the _`JAR Selection`_ dialog, add from folder _`..../workspace/scriptic/lib`_ the files _`sawa.jar`_ and _`scripticvm.jar`_

Using the file manager outside Eclipse, or using the command line, copy the **folder** _`Examples`_ from _`..../workspace/scriptic/`_ into folder _`..../workspace/Scriptic Demo/src`_

Create a Run Configuration for the Scriptic compiler:
  * In Eclipse, issue the menu command _`Run|Open Run Dialog`_ to open the dialog named _`Create, manage, and run configurations`_
  * Double click _`Java Application`_; this creates a new configuration
  * Change the name into _`Compile Scriptic sources`_
  * In the _`Main`_ tab, next to the entry field _`Main class`_, click the `Search...` button
    * In the _`Select Main Type`_ dialog, select _`Sawa - scriptic.tools`_, and close the dialog
  * In the _`Arguments`_ tab, specify for Program arguments: _`-o bin src/Examples/*.sawa`_
  * In the _`Classpath`_ tab, click _`User Entries`_ and **then** button _`Add External JARs...`_
    * in the dialog, select the Java runtime archive _`rt.jar`_ from the Java installation of your system, e.g. in folder _`/usr/lib/jvm/jre-1.7.0-icedtea.x86_64/lib`_ or _`C:\Program Files\Java\jre6\lib`_. Note: on MacOS, the jar is probably named `classes.jar`
  * Click the button _`Run`_ at the bottom of the _`Create, manage, and run configurations`_ dialog; this should yield output in the Console panel like:
```
Scriptic/Java compiler; open source software - GNU General Public License

   218 Initialized
   274 Parsed              src/Examples/LifeFrame.sawa
    10 Parsed              src/Examples/LookupFrame.sawa
    12 Parsed              src/Examples/QSim.sawa
    12 Parsed              src/Examples/LookupFrame2.sawa
   130 Inspected           LifeFrame
     2 Inspected           LookupFrame
     2 Inspected           QSim
     2 Inspected           LookupFrame2
   105 Checked             LifeFrame
     2 Checked             LookupFrame
     2 Checked             QSim
     1 Checked             LookupFrame2
   115 Resolved            LifeFrame
   179 Generated code      LifeFrame
   156 Written             bin/Examples/LifeFrame.class
    37 Resolved            LookupFrame
    10 Generated code      LookupFrame
    50 Written             bin/Examples/LookupFrame.class
    31 Resolved            QSim
    16 Generated code      QSim
    40 Written             bin/Examples/QSim.class
    26 Resolved            LookupFrame2
    10 Generated code      LookupFrame2
    15 Written             bin/Examples/LookupFrame2.class
  1458 done
```

Now create run configurations for the Examples, for project _`Scriptic Demo`_:
  * _`QSim`_ with main class _`Examples.QSim`_ and arguments `1`
  * _`LookupFrame`_ with main class _`Examples.LookupFrame`_
  * _`LookupFrame2`_ with main class _`Examples.LookupFrame2`_
  * _`Life`_ with main class _`Examples.LifeFrame`_
Run these.

# Troubleshooting #
Every once  in a while you may encounter  a ClassNotFoundException, when you want to execute a Scriptic program using an Eclipse run configuration. These may even occur when everything seems right on the code level.
To solve such issues, try the following steps:
  * Invoke the menu command `Project|Clean`
  * Recompile the Scriptic sources
In many cases this helps