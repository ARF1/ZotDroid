ZotDroid
========

A Zotero Client for Android devices
-----------------------------------

[Zotero](http://www.zotero.org) is a fantasic program for anyone doing any kind of research. Seriously! I'm a research student in Computer Science and Bioinformatics and I could not do what I do without Zotero. Accept no substitutes! :)

I like to use my tablet to read PDFs and similar so I wanted an Android client. There wasn't one on the market that I liked so I wrote my own.

Feedback is more than welcome, either here or via email *me@benjamin.computer*

Current Version
---------------

0.7

Building and Testing
--------------------

This is an AndroidStudio project and should just drop right in. However, you do need to download [Signpost](https://github.com/mttkay/signpost) as this is required for the OAUTH stuff. I build this using Maven and then drop in the Jar files once built.

If you follow the instructions on that page, you should get a set of jars that you can place in the lib directory within this project. Make sure they are added as dependencies and it should all compile fine.


Things that are done
--------------------

* Pagination (still in progress)
* Reading a fresh copy of a Zotero Library
* Downloading an attachment via the WebDav interface
* Basic search on all fields
* Selecting a collection and viewing the items for just that collection
* Incremental syncing
* Backing up via SQLite DB
* Support for Zotero cloud storage
* Storing the database on an SDCard
* Option as to where to save attachments
* Icons to show if an attachment is downloaded already (partial)
* Multiple Author records


Things still to do
------------------
* More testing!
* Tag support
* Label support
* Adding notes
* Removing records
* Removing collections
* Modifying records
* Sorting via multiple options such as date
* More robust recovery from internet problems
* Better UX design
* Whatever the greater Zotero community wants

Acknowledgements
----------------

* The [Zotero](https://www.zotero.org) crew for making a wicked awesome program!
* ZotDroid makes use of [Signpost](https://github.com/mttkay/signpost). This lives in the external directory for these who wish to build ZotDroid themselves.
* The [Zandy](https://github.com/avram/zandy) project, for an idea on how OAUTH works.
* [XListView-Android](https://github.com/Maxwin-z/XListView-Android) - For the very handy list dragging animations and event handling.

Licence
-------

    ZotDroid - An Android client for Zotero
    Copyright (C) 2017  Benjamin Blundell

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.


