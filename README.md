# bootcamp [![Build Status][badge]][build]

Bootcamp is a server and clientside application that harvests data about
existing [Boot][boot] projects from GitHub and displays it in the browser.

## Development

For development, I like to run the frontend and backend in different processes
so that one can be restarted if necessary without stopping the other.

```bash
# start the backend
boot serve-backend wait
```

And in another terminal:

```bash
boot run-frontend
```

If you would rather just run the application in one terminal only,
you can combine the two with the `dev` task:

```bash
boot dev
```

You can view the generated content by opening
[http://localhost:8484](http://localhost:8484)
in your browser.

## Status

### Outstanding Issues

#### Zombie socket connections

- Chrome/Chromium have an issue where refreshing the browser does not
send a socket-close signal to the websocket server. As a result, the
socket connections store can accumulate stale connections.
- Relatedly, put!s are accumulating on the server when trying to send
data-changed messages to all client connections
- 

## License

Copyright © 2015 Murphy McMahon

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[badge]:            https://travis-ci.org/pandeiro/bootcamp.png?branch=devel
[build]:            https://travis-ci.org/pandeiro/bootcamp
[boot]:             https://github.com/boot-clj/boot
[installboot]:      https://github.com/boot-clj/boot#install
