# bootcamp [![Build Status][badge]][build]

Bootcamp is a server and clientside application that harvests data about
existing [Boot][boot] projects from GitHub and displays it in the browser.

Status: Working, still WIP

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
[http://localhost:8484](http://localhost:8484)
in your browser.

## License

Copyright © 2015 Murphy McMahon

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[badge]:            https://travis-ci.org/pandeiro/bootcamp.png?branch=devel
[build]:            https://travis-ci.org/pandeiro/bootcamp
[boot]:             https://github.com/boot-clj/boot
[installboot]:      https://github.com/boot-clj/boot#install
