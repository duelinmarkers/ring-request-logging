# ring-request-logging

A [ring](https://github.com/mmcgrana/ring) middleware for logging requests.

You can find [the latest release at Clojars](https://clojars.org/com.duelinmarkers/ring-request-logging).

[![Build Status](https://secure.travis-ci.org/duelinmarkers/ring-request-logging.png)](http://travis-ci.org/duelinmarkers/ring-request-logging)

## Usage

Logging is done using [clojure.tools.logging](https://github.com/clojure/tools.logging),
so you can use any logging framework it supports.

The intention of the design of ring-request-logging is that you can achieve
everything from reasonable prod logging to the most verbose debug logging by
just adjusting the log config (as opposed to making code changes and
reloading).

The logger used is "com.duelinmarkers.ring-request-logging", so that's the
logger to reconfigure if you want more or less logging.

The public API is the `wrap-request-logging` function.

    (ns example
      (:require
        [ring.middleware.params :refer (wrap-params)]
        [ring.middleware.keyword-params :refer (wrap-keyword-params)]
        [com.duelinmarkers.ring-request-logging :refer (wrap-request-logging)]))
    
    (defn ring-app [request] {:status 200 :body "Worst app ever!"})
    
    (def logged-app
      (wrap-request-logging ring-app
                            :param-middleware [wrap-params
                                               wrap-keyword-params]))

On each request, this middleware will do the following:

1.  The start of each request will be logged at `:info` level.
2.  The entire request map will be logged at `:trace` level. (You almost
    certainly only want that on for local testing.)
3.  The supplied `:param-middleware` will be run in the provided order.
4.  The `:params` of the request will be logged at `:debug` level.
5.  The wrapped app will be invoked.
6.  Anything thrown by the app will be logged at `:error` level and rethrown
    (to be caught by surrounding middleware or the adapter).
7.  Assuming nothing was thrown, the end of each request will be logged at
    `:info` level.

The param-middleware is incorporated so that the start of request processing
can be logged as early as possible, before reading the request body, but
params can still be logged.

See the doc string for `wrap-request-logging` for options.

## License

Copyright © 2012 John D. Hume

Distributed under the Eclipse Public License, the same as Clojure.
