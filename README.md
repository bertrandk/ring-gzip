# Ring-Gzip

Ring middleware for gzip compression.

## Installation

To use this libary, add the following to your Leingingen `:dependencies`:

    [bk/ring-gzip "0.3.0"]

## Usage

The `wrap-gzip` middleware will compress any supported responses before
sending them to compatible user-agents. Typically, the middleware would
be applied to your Ring handler at the top level (i.e. as the last form
in a `->` function).

```clojure
(ns app.core
  (:use  [ring.middleware.gzip]
         [ring.middleware params
                          keyword-params
                          nested-params]))

(def app
  (-> handler
    (wrap-keyword-params)
    (wrap-nested-params)
    (wrap-params)
    (wrap-gzip)))
```

## License

Copyright © 2013 Bertrand Karerangabo

Distributed under the MIT License (see LICENSE).
