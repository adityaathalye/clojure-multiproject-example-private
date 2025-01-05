# com.bombaylitmag/mothra

## The TBLM Process

### OVERVIEW:

    "We are stuck with technology when all we really want is just stuff that works. How do you recognize something that is still technology? A good clue is if it comes with a manual." --Douglas Adams

When an author submits a manuscript to us, its status is marked as ACTIVE, the manuscript is added to Round 1 (slush pile), and an editor is assigned to it. The situation is noted as "UNREAD".

There are four Rounds in our evaluation process:

A. Round 1: slush pile evaluation
B. Round 2: long list evaluation
C. Round 3: short list evaluation
D. Round 4: select the accepts.

In each Round, a manuscript can be in one of six situations:

- UNREAD -- editor hasn't read the manuscript yet.
- READ -- editor is reading the manuscript, but hasn't made any final decisions.
- REJECT -- editor decides on a "standard" reject. No further evaluation.
- PERS. REJECT -- editor decides on a personalised reject. No further evaluation.
- NEXT ROUND -- editor okays the manuscript for further evaluation.
- ALERT ADMIN -- the manuscript hasn't followed the submission criteria.

Note #1: If the manuscript is currently in Round 4, then "next round" = "accept the manuscript".

Note #2: Though a manuscript can be marked for the "NEXT ROUND", the system waits for all Round 1 decisions to be in before assigning the next set of editors.

Q1: What happens if an author withdrawns their manuscript?
ANS: The manuscript's status is marked as WITHDRAWN, and it is removed from any further consideration.

Q2: What happens if an author partially withdrawns their manuscript?
ANS: The manuscript's status remains ACTIVE but an appropriate note can be made to indicate which items have been withdrawn.

Q3: What happens if an editor discovers something wrong with the manuscript? Say, the author has accidentally specified the wrong category for their submission?
ANS: The editor marks the situation as "ALERT ADMIN" and takes a chai break.

### My Submissions
This panel is divided into two parts. The left part shows a table of submissions. By default, the table lists all submissions assigned to the specified editor and specified category. Just above the table is a button that toggles between "Show Filters" and "Hide Filters". It can be used to narrow the list of submissions shown in the table. In the table, the current row (submission) selected for evaluation is highlighted in light blue.

The right part (named DETAILS) is a tabbed panel. It shows the details of each selected row. These details are fairly self-explanatory, but five items are worth mentioning.

- The editor can add their (optional) comments about the submissions.
- They can modify the situation of the current submission. By default the submissions are marked UNREAD. The editor can change this situation to READ or REJECT or PERSONAL REJECT or move the submission to the NEXT ROUND.
- They can assign an (optional) score. This subjective score/rating/ranking is useful to indicate one's enthusiasm for a particular submission.
- They can associate an (optional) icon. This icon is simply a visual cue that has some meaning for the editor.
- Finally, the far upper-right corner of the DETAILS panel shows a "progress meter". It shows what percentage of the submissions assigned to the current editor and category have been moved to the NEXT ROUND.

Note #1: The changes an editor makes is only recorded after they click the SUBMIT button.

Note #2: If, in any round, an editor changes the submission's situation to NEXT ROUND and then submits the change, then no further changes to this particular submission.

### Admin
As with the My Submissions panel, the top part of the screen is divided into a left part showing a list of ALL submissions in the system, and a right part which contains a form. The form allows a submission's status to be changed (typically, from ACTIVE to WITHDRAWN or ERROR). It allows a few other changes. These are not evaluative changes, but rather, administrative changes.

The ADMIN panel also contains a means to assign editors (automatically) to submissions who've marked for shift to the next round. So, for example, all the Round 1 submissions in the Fiction category marked as "NEXT ROUND", will be shifted to Round 2, and each submission assigned an editor.

### Statistics
It is useful to have an idea of how we're doing as a team and individually. But the exact stats we wish to collect is still to be determined, so for now, we just have a "progress meter" display of the number of fully evaluated submissions in each round and in each category.


## Installation

Download from https://github.com/bombaylitmag/mothra

## Usage

FIXME: explanation

Run the project directly, via `:exec-fn`:

    $ clojure -X:run-x
    Hello, Clojure!

Run the project, overriding the name to be greeted:

    $ clojure -X:run-x :name '"Someone"'
    Hello, Someone!

Run the project directly, via `:main-opts` (`-m com.bombaylitmag.mothra`):

    $ clojure -M:run-m
    Hello, World!

Run the project, overriding the name to be greeted:

    $ clojure -M:run-m Via-Main
    Hello, Via-Main!

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build an uberjar (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the uberjar in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

If you don't want the `pom.xml` file in your project, you can remove it. The `ci` task will
still generate a minimal `pom.xml` as part of the `uber` task, unless you remove `version`
from `build.clj`.

Run that uberjar:

    $ java -jar target/com.bombaylitmag/mothra-0.1.0-SNAPSHOT.jar

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2024 Adi

_EPLv1.0 is just the default for projects generated by `deps-new`: you are not_
_required to open source this project, nor are you required to use EPLv1.0!_
_Feel free to remove or change the `LICENSE` file and remove or update this_
_section of the `README.md` file!_

Distributed under the Eclipse Public License version 1.0.
