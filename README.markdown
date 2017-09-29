ZotDroid
========

A Zotero Client for Android devices
-----------------------------------

[Zotero](http://www.zotero.org) is a fantasic program for anyone doing any kind of research. Seriously! I'm a research student in Computer Science and Bioinformatics and I could not do what I do without Zotero. Accept no substitutes! :)

I like to use my tablet to read PDFs and similar so I wanted an Android client. There wasn't one on the market that I liked so I wrote my own.

Feedback is more than welcome, either here or via email *oni at section9.co.uk*

Current Version
---------------

0.3

Building and Testing
--------------------

This is an AndroidStudio project and should just drop right in. However, you do need to download [Signpost](https://github.com/mttkay/signpost) as this is required for the OAUTH stuff.

If you follow the instructions on that page, you should get a set of jars that you can place in the lib directory within this project. Make sure they are added as dependencies and it should all compile fine.


Things that are done
--------------------

* Reading a fresh copy of a Zotero Library
* Downloading an attachment via the WebDav interface
* Basic search on all fields
* Selecting a collection and viewing the items for just that collection
* Incremental syncing
* Backing up via SQLite DB
* Support for Zotero cloud storage

Things still to do
------------------
* More testing!
* Storing the database on an SDCard
* Tag support
* Label support
* Option as to where to save attachments
* Icons to show if an attachment is downloaded already
* Adding notes
* Removing records
* Removing collections
* Modifying records
* Multiple Author records
* Sorting via multiple options such as date
* More robust recovery from internet problems
* Better UX design
* Whatever the greater Zotero community wants

Acknowledgements
----------------

* The [Zotero](https://www.zotero.org) crew for making a wicked awesome program!
* ZotDroid makes use of [Signpost](https://github.com/mttkay/signpost). This lives in the external directory for these who wish to build ZotDroid themselves.
* The [Zandy](https://github.com/avram/zandy) project. Whilst this didn't work right for me, it did get me started on this road.
