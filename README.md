# Wikistruct 0.1.1

Get yourself some nice data structures from *any* (Media)Wiki page.

## NOTE
Wikistruct as not yet been deployed to clojars! A release should be available
before the end of december. 

## Features

Wikistruct is a clojure wrapper around the /w/api.php MediaWiki API that converts
a wiki page into a clojure data structure. It is a great way to get information
about entities that exist across multiple Wiki projects (e.g:
[Wikipedia Montreal](http://en.wikipedia.org/wiki/Montreal) and 
[Wikivoyage Montreal](http://wikivoyage.org/wiki/Montreal).)

## Quickstart

### Leiningen
Add the following line to your leiningen dependencies:

```clojure
[wikistruct "0.1.1"]
```

and require the library in your project like so:

```clojure
(ns myproject.core
  (:require [wikistruct.core :as wk]))
```

### Fetching content
To get the desired content, use the article function by giving it a user-agent
string along with the url of the page:

```clojure
; will fetch the whole Quebec city article.
(wk/article "wikistruct 0.1.1 used by email@example.com" 
            "http://en.wikipedia.org/wiki/Quebec_City")
```

the user-agent string ideally contains an email address so that you can be 
contacted if a problem arise (as per the MediaWiki 
[API Etiquette](http://www.mediawiki.org/wiki/API:Etiquette). The result of the
previous function call should look something like this:

```clojure
{:abstract "Quebec (/kɨˈbɛk/; French: Québec [kebɛk]) ..."
 :lang "en"
 :pageid 100727
 :depiction "http://upload.wikimedia.org/wikipedia/commons/2/24/Quebec_City_Montage.png"
 :sections [{:title "History" :sections [{:title "Early history"
                                          :text  "Quebec City is ..."}]}]
 ...
}
```

There are more properties returned from wk/article and you should try fetching
a few articles to see them.

Imagine now that you are still interested in Quebec City, but from a 
travaller's perspective. You could query [wikivoyage](http://wikivoyage.org) 
for the city article in just the same way you queried wikipedia:

```clojure
; the following call
(wk/article "wikistruct 0.1.1 used by email@example.com"
            "http://en.wikivoyage.org/wiki/Quebec_City")

; would return something similar to this
{:abstract "Quebec City (French: Québec) is the "national" capital of ..."
 :lang "en"
 :pageid 28931
 :depiction " ... "
 :sections [ ... ]
 ...
}
```

### Using as a standalone .jar
Wikistruct can be used as a standalone application that takes a list of
url as input. It will fetch the content from those url and
print the resulting data structure serialized as json documents to stdout. 
Building  and using the standalone .jar is very straightforward:

    git clone https://github.com/jopela/wikistruct 
    cd Wikistruct 
    lein uberjar
    cd ./target


## Upcoming features
In the future, there are plans for adding the following features to wikistruct .

+ Adding the links to other wiki pages in the result json document (usefull for 
crawling mediawiki).
+ Adding the links to images in the result json document.
+ Adding support for returning semantic content stored in infoboxes.
+ Adding editing capabilities to wikistruct (for machine editing of wiki's).
+ Better support for gracefull failure when url does not exist, when network
down etc.

## Help support development
Help me support Wikistruct by donating bitcoin at:

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
To fix a bug, there is a temporary work-around implemented by adding a single 
newline character to the wiki-creole text returned by the mediawiki API. This 
changes nothing to the real content except by adding an empty newline in the 
last section of every article. This is a bug in the context-free grammar itself 
and will fixed in a later release. 

## License
Copyright © 2013 Jonathan Pelletier (jonathan.pelletier1@gmail.com)

Distributed under the GPL version 3, 29 June 2007
