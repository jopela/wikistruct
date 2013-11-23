# Wikison 0.1.1

Get yourself some nice json documents from *any* (Media)Wiki page.

## Features

Wikison is a clojure wrapper around the /w/api.php MediaWiki API that converts
a wiki page into a json document. It is a great way to get information about
entities that exist across multiple Wiki projects (e.g:
[Wikipedia Montreal](http://en.wikipedia.org/wiki/Montreal) and 
[Wikivoyage Montreal](http://wikivoyage.org/wiki/Montreal)   
. A short list of features:

+ Works across *all* MediaWiki backed projects (e.g: wikipedia.org,
wikivoyage.org, species.wikimedia.org, and even your own wiki's !).
+ Parses the wiki-creole article text for you and generate a tree-like
document that preserves the hierarchy of the article (subsections as
children of sections for all section levels)
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
To get the desired content, use the article function by giving it a user-agent
string along with the url of the page:

    ; will fetch the whole Quebec city article.
    (wk/article "wikison 0.1.1 used by email@example.com" 
                "http://en.wikipedia.org/wiki/Quebec_City")

the user-agent string ideally contains an email address so that you can be 
contacted if a problem arise (as per the MediaWiki 
[API Etiquette](http://www.mediawiki.org/wiki/API:Etiquette). The result of the
previous function call should look something like this:

    {:abstract "Quebec (/kɨˈbɛk/; French: Québec [kebɛk]) ..."
     :lang "en"
     :pageid 100727
     :depiction "http://upload.wikimedia.org/wikipedia/commons/2/24/Quebec_City_Montage.png"
     :sections [{:title "History" :sections [{:title "Early history"
                                              :text  "Quebec City is ..."}]}]
     ...
    }

There are more properties returned from wk/article and you should try fetching
a few articles to see them.

Imagine now that you are still interested in Quebec City, but from a 
travaller's perspective. You could query [wikivoyage](http://wikivoyage.org) 
for the city article in just the same way you queried wikipedia:

    ; the following call
    (wk/article "wikison 0.1.1 used by email@example.com"
                "http://en.wikivoyage.org/wiki/Quebec_City")

    ; would return something similar to this
    {:abstract "Quebec City (French: Québec) is the "national" capital of ..."
     :lang "en"
     :pageid 28931
     :depiction " ... "
     :sections [ ... ]
     ...
    }

### Using as a standalone .jar
Wikison can be used as a standalone application that takes a list of
url as input. It will fetch the content from those url and
print the resulting json document to stdout. Building the standalone .jar is 
straighforward:

    git clone https://github.com/jopela/wikison
    cd wikison
    lein uberjar


## Upcoming features
In the future, there are plans for adding the following features to Wikison.

+ Adding the links to other wiki pages in the result json document (usefull for 
crawling mediawiki).
+ Adding the links to images in the result json document.
+ Adding support for returning semantic content stored in infoboxes.
+ Adding editing capabilities to wikison (for machine editing of wiki's).
+ Better support for gracefull failure when url does not exist, when network
down etc.

## You can help Wikison development
Help the continued support and enhancement of Wikison by donating bitcoin at:
**jopela**
184jV73JDd5Y4FQVcrLDrNS1fxWPxaiVgV

## Special Note
This is a first clojure project for me, and some of the code construct
may not feel as idiomatic as they could be to the seasonned clojure hacker.
All improvement suggestions and contributions are welcomed with open arms!

## Special mentions
Wikison was built using the awesome 
[Instaparse](https://github.com/Engelberg/instaparse) parser generator. 
Make sure to check this project out!

## Known issues
There is a temporary work-around implemented by adding a single newline to 
character to the wiki-creole text returned by the mediawiki API. This changes 
nothing to the real content except by adding an empty newline in the last 
section of every article. This is a bug in the context-free grammar itself and 
will fixed in a later release. 

## License
Copyright © 2013 Jonathan Pelletier

Distributed under the GPL version 3, 29 June 2007
