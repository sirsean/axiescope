# axiescope

First there was [axie](https://github.com/sirsean/axie), which let you interact with the Axie Infinity API through a CLI app.

But that's not necessarily convenient enough for everyone. So now `axiescope` is a web app that is more accessible.

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
