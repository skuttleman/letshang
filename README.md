# Let's Hang

A web app for simplifying the grueling process of organizing a simple get together between friends.

## Development


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
