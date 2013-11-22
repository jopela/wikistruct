# Wikison 0.1

Query the vast amount of knowledge stored in (Media)Wiki backed project.

## Features

Wikison is a clojure wrapper around the /w/api.php MediaWiki API that convert
a wiki page into a json document.

+ Works across *all* MediaWiki backed projects (e.g: wikipedia.org,
wikivoyage.org, species.wikimedia.org and even your own wiki's !).
+ Parses the wiki-creole article content for you and produces a 
tree-like, hierarchy-preserving json document (e.g: with abstract at the root
of the document and sections as childs).
+ Can be used as a clojure library or as a .jar stand-alone application that
takes a list of wiki url as input and print the json documents on stdout.

## Quickstart

Add the following line to your leiningen dependencies:

    [wikison "0.1.1"]

and require the library in your project like so:

    (ns myproject.core
      (:require [wikison.core :as 

### Using as a standalone .jar

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
This is a first clojure project for me and therefore some code construct
may not feel as idiomatic as they could be to more seasonned 

## Special mentions
Wikison was built using the awesome 
[Instaparse](https://github.com/Engelberg/instaparse) parser generator. 
Check it out at https://github.com/Engelberg/instaparse.

## Known issues
There is a temporary work-around implemented by adding a single "\n" to the 
wiki-creole text returned by the mediawiki API. This changes nothing to the
real content except by adding an empty newline in the last section of every
article. This is a bug in the context-free grammar itself and will fixed in a
later release. 

## License

Copyright © 2013 Jonathan Pelletier

Distributed under the GPL version 3, 29 June 2007
