# Wikison 0.1.1

Get yourself some nice json documents from *any* (Media)Wiki pages.

## Features

Wikison is a clojure wrapper around the /w/api.php MediaWiki API that convert
a wiki page into a json document. It is a great way to get information about
entities that exist across multiple Wiki projects (e.g:
[Wikipedia Montreal](http://en.wikipedia/wiki/Montreal) and [Wikivoyage Montreal](http://wikivoyage.org/wiki/Montreal)   
. A short list of features:

+ Works across *all* MediaWiki backed projects (e.g: wikipedia.org,
wikivoyage.org, species.wikimedia.org and even your own wiki's !).
+ Parses the wiki-creole article text for you using a simple context-free 
grammar and evaluates the syntax-tree into a json document.
of the document and sections as childs).
+ Can be used as a clojure library or as a .jar stand-alone application that
takes a list of wiki url as input and print the json documents on stdout.

## Quickstart

### Leiningen
Add the following line to your leiningen dependencies:

    [wikison "0.1.1"]

and require the library in your project like so:

    (ns myproject.core
      (:require [wikison.core :as wk]))

### Fetching content
You can get the 

### Using as a standalone .jar
Wikison can be used as a standalone application that takes a list of
urls as input. It will then fetch content from those url and
print the resulting json document to stdout. It also require you to specify a 
user-agent string that will be sent along with the request to the MediaWiki.
Building the standalone .jar would look something like this:

    git clone 

### Output Format



## Upcoming features

In the future, there are plans for adding the following features to Wikison.

+ Adding the links to other wiki pages in the result json document (usefull for 
crawling mediawiki).
+ Adding the links to images in the result json document.
+ Adding support for returning semantic content stored in infoboxes.
+ Better support for gracefull failure when url does not exist, when network
down etc.

## You can help Wikison development
Help the continued support and enhancement of Wikison by:
...

## Special Note
This is a first clojure project for me and some of the code construct
may not feel as idiomatic as they could be to the seasonned clojure hacker.
All improvement suggestions and contributions are welcomed with open arms!

## Special mentions
Wikison was built using the awesome 
[Instaparse](https://github.com/Engelberg/instaparse) parser generator. 
Make sure to check it out!

## Known issues
There is a temporary work-around implemented by adding a single newline to 
character to the wiki-creole text returned by the mediawiki API. This changes 
nothing to the real content except by adding an empty newline in the last 
section of every article. This is a bug in the context-free grammar itself and 
will fixed in a later release. 

## License

Copyright © 2013 Jonathan Pelletier

Distributed under the GPL version 3, 29 June 2007
