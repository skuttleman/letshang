# Let's Hang

A web app for simplifying the grueling process of organizing a simple get together between friends.

## Development

Requires a local install of Java and the JRE and leiningen. The app uses a postgres
database (local or otherwise).

### Clone the repo
```bash
$ git clone git@github.com:skuttleman/letshang.git
```
### Setup the environment

```bash
$ echo \
'{"JWT_SECRET" "keyboard-cat"
 "USER_AGENT" "LetsHang/1.0"
 "DB_USER" "user"
 "DB_PASSWORD" "password"
 "DB_HOST" "www.db-host.net"
 "DB_PORT" "5432"
 "DB_NAME" "lets_hang"}' > .lein-env
```

### Initialize the Database

```bash
$ lein migrations migrate
$ lein migrations seed
```

### Run It

The app runs on `PORT` 3000 and starts a development nREPL on `NREPL_PORT` 7000. Figwheel starts a ClojureScript nREPL
on port `7888`. After connecting to the nREPL and opening the app in a browser, run `(cljs-repl)` to connect to the
browser.

```bash
$ lein cooper
```

Optionally, you can specify the ports.

```bash
$ PORT=1234 NREPL_PORT=4321 lein cooper
```
