# bootcamp [![Build Status][badge]][build]

Example project using [the boot build tool][boot] with the [boot-cljs],
[boot-cljs-repl], and [boot-reload] tasks. Derived from [boot-cljs-example].

## Prepare

[Install boot][installboot].  Then, in a terminal:

```bash
boot -u
```

This will update boot to the latest stable release version. Since boot is
pre-release software at the moment, you should do this frequently.

## Development

In a terminal do:

```bash
boot dev
```

You can view the generated content by opening
[http://localhost:8080/index.html](http://localhost:8080/index.html)
in your browser.

## Start Browser REPL

With the build pipeline humming in the background, you can connect to the running nREPL
server with either your IDE or at the command line in a new terminal:

```bash
boot repl --client
```

or in Emacs:

```
M-x cider-connect
```

Then, you can start a CLJS REPL:

```clojure
boot.user=> (start-repl)
```

Reload the page in your browser.  Your REPL is now connected to the page. (If it's not, try touching
one of the *.cljs files to trigger recompilation, refresh the page and try again.)

## License

Copyright © 2015 Murphy McMahon

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[badge]:            https://travis-ci.org/pandeiro/bootcamp.png?branch=devel
[build]:            https://travis-ci.org/pandeiro/bootcamp
[boot-cljs-example]: https://github.com/adzerk/boot-cljs-example
[boot]:             https://github.com/boot-clj/boot
[cider]:            https://github.com/clojure-emacs/cider
[boot-cljs]:        https://github.com/adzerk/boot-cljs
[boot-cljs-repl]:   https://github.com/adzerk/boot-cljs-repl
[boot-reload]:      https://github.com/adzerk/boot-reload
[installboot]:      https://github.com/boot-clj/boot#install
[gclosure]:         https://developers.google.com/closure/compiler/
