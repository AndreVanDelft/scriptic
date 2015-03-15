## Scriptic: easy event-driven and concurrent programming ##

Scriptic is an extension to the Java programming language, aimed at event-driven and concurrent programming, and discrete event simulations.
See the documents in the [download area](http://code.google.com/p/scriptic/downloads/list) for more information. A good start is the [White Paper](http://scriptic.googlecode.com/files/Scriptic%20White%20Paper.pdf). More examples are in the document [Introducing Scriptic](http://scriptic.googlecode.com/files/Introducing%20Scriptic.pdf).

As of writing, a successor of Scriptic is being developed, named [SubScript](http://code.google.com/p/subscript/), also present at Google Code. Here is a [PowerPoint presentation](http://code.google.com/p/subscript/downloads/detail?name=Event-driven%20and%20Concurrent%20Programming.ppt) of a lecture at Amsterdam University for the course Thread Algebra.


## Physical Dimensions Extension ##

The current Scriptic compiler also supports the specification of physical dimensions for numbers; see [this document](http://code.google.com/p/scriptic/downloads/detail?name=A%20Java%20extension%20with%20support%20for%20dimensions.pdf).

## Getting Started ##

An [quick installation guide](http://code.google.com/p/scriptic/wiki/QuickStartWithEclipse) for use with Eclipse, and [student assignments](http://code.google.com/p/scriptic/wiki/Assignments) are available at [the Scriptic Wiki](http://code.google.com/p/scriptic/w/list).

With the Jar archives in the Downloads area, you can compile and run Scriptic programs from the command line; see the [Wiki page](http://code.google.com/p/scriptic/wiki/commandLineOperation) on that subject

Likewise for compiling and running [from the Eclipse IDE](http://code.google.com/p/scriptic/wiki/operationFromEclipse).

## Quick look ##

For a quick look consider the following use case: we need a simple Java-based program to lookup items in a database, based on a search string.

![http://scriptic.googlecode.com/files/Lookup1.png](http://scriptic.googlecode.com/files/Lookup1.png)

The user can enter a search string in the text field and then press the Go button. This will at first put a “Searching…” message in the text area at the lower part. Then the actual search will be performed at a database, which may take a few seconds. Finally the results from the database are shown in the text area.

In Scriptic you can program that sequence of events and actions in an intuitively clear way:
```
searchSequence = searchCommand; 
                 showSearchingText;
                 searchInDatabase;
                 showSearchResults
```
Here searchCommand would represent the event of the user pressing the button. It is refined with a reusable script `action`:
```
searchCommand  = action(searchButton)
```
The other _script calls_ are refined likewise. `showSearchingText` and `showSearchResults` write a message in the GUI, which will be executed in the GUI thread. `searchInDatabase` does the database search, but that happens in a background thread.

```
showSearchingText = @swing: {showSearchingText()}
showSearchResults = @swing: {showSearchResults()}
searchInDatabase  = {* searchInDatabase() *}
```

As a bonus, the button is enabled exactly when clicking it would start the search.

If you would to program this functionality in plain Java, the resulting code will be much more complex. Even without enabling and disabling the button, the code would look like:

```
private void searchButton_actionPerformed() {
    showSearchingText();
    new Thread() {
        public void run() {
            searchInDatabase();
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        showSearchResults();
                    }
                }
            );
        }
    }.start();
}
```

### Extending the program ###

It is easy to extend the functionality of this program. For instance, the search action may also be triggered by the user pressing the Enter key in the search text field (`searchTF`). For this purpose we can adapt the `searchCommand`. Another user command could be to cancel an ongoing search in the database. For this the user could press a Cancel button, or press the Escape key. Finally the user may want to exit the application by pressing an Exit button, rather than clicking in the closing box. Moreover, he should then be presented a dialog where he can confirm the exit.

![http://scriptic.googlecode.com/files/Lookup2.png](http://scriptic.googlecode.com/files/Lookup2.png)

```
searchCommand = action(searchButton) 
              + vkey(searchTF, KeyEvent.VK_ENTER !)              

cancelCommand = action(cancelButton) 
              + vkey(searchTF, KeyEvent.VK_ESCAPE!)

  exitCommand = action(exitButton) + windowClosing(this)
```
Just like `action`, `vkey` and `windowClosing` are predefined scripts.

Here the plus symbol denotes choice, just like the semicolon denotes sequence. There are 7 other operators like these, most of which express a specific flavour of parallelism. The exclamation mark following `KeyEvent.VK_ENTER`, expresses that only pressing the Enter key, and no other, will make the virtual key event in `vkey` happen.

Note that the default behaviour of the orange Close box in Swing is to rudely close the application. That should be suppressed; the GUI code for the program should for that purpose contain a line such as
```
     setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
```
After the exit command, a confirmation dialog should come up, in the Swing thread. If the user confirms, then the program should end. After the cancel command, an applicable text should be shown in the text area:
```
exit          =   exitCommand; @swing: while(!confirmExit())
cancelSearch  = cancelCommand; @swing: {showCanceledText()}
```
This exit script is easily added to the live script:
```
live            = searchSequences || exit
searchSequences = ...; searchSequence
```
The double bar `||` denotes or-parallelism: both operands happen, but when one operand terminates successfully the parallel composition also terminates successfully. The double bar is here analogous to its usage in boolean expressions. The same holds for 3 other parallel operators: `&&`, `|` and `&`.

The 3 dots (`...`) are equivalent to `while(true)`. Two dots (`..`) would create a loop with an optional exit, a bit comparable to the asterisk in regular expressions.

To fit in a call to the `cancelSearch` script, we have to split up the script `searchSequence`:
```
searchSequence = searchCommand; (searchAction / cancelSearch)
        
searchAction   = showSearchingText;
                 searchInDatabase;
                 showSearchResults 
```
This way the Cancel button will only become enabled after the search command has been issued, and it will be disabled again after the search results have been shown.

The slash symbol (`/`) denotes breaking behavior: the left hand side happens, but the right hand side may take over.

## More benefits ##

Scriptic offers more benefits than just easy event handling and threading:

  * Conciseness: the control flow specification takes usually much less space than in plain Java. Even a single line can express significant portions of control flow

  * Logic parallel behaviour: flavours of and-parallelism and or-parallelism turn out to allow elegant specification of practical behaviour patterns

  * Mathematical format of script expressions: sequences, choices and parallelism are all written down as expressions in a mathematic format, with operators such as + and ;. The basic syntax of such expressions is therefore already familiar. Moreover, the programmer may reason about the semantics of expressions by applying algebraic laws that hold for the operators.

  * Convenience features: e.g., a script for describing a key press event, may have an output parameter for the key character; such a script may be called with  a “forcing” character value, so that the call only succeeds when that key character is pressed

  * Support for discrete event simulations: the fragments of Java code have an attributed number named duration, giving Scriptic programs a notion of simulation time

  * Good integration with Java: scripts may be called from Java, and Java code may be executed by scripts. Scripts are class members, comparable to methods. They are inherited, and may be overridden in subclasses.

  * Fast and small virtual machine: the design of the language was driven by repeated evaluation of its practical applicability, but also with ease and efficiency of implementation in mind. Running a Scriptic program requires a virtual machine that is currently about 125 Kb. The language has about 50 features, of which 7 have already been shown earlier in this paper. Each feature required therefore on average less than 3 Kb. The virtual machine has been optimized for speed. It does in general not slow down noticeably the interaction between a program and its user

  * Scriptic eases programming in the model-view-controller paradigm (MVC): the model is specified in plain Java, the view is created using a GUI painter, and the controller is specified conveniently in Scriptic. The resulting program structure resembles lasagne more than spaghetti.

## Getting involved ##

There is still a lot to do for Scriptic; see the [roadmap](http://scriptic.googlecode.com/files/Scriptic%20Roadmap.pdf)
If you want to participate in the project, contact the project owner.