(ns ring.middleware.gzip-test
  (:use clojure.test
        ring.middleware.gzip)
  (:require [clojure.java.io :as io])
  (:import (java.io PipedInputStream)))

(def short-string "Note that gzipping is only beneficial for larger
                  resources. Due to the overhead and latency of
                  compression and decompression, you should only gzip
                  files above a certain size threshold; Google
                  recommends a minimum range between 150 and 1000 bytes.
                  Gzipping files below 150 bytes can actually make them
                  larger. According to Akamai, the reasons 860 bytes is
                  the minimum size for compression is twofold: (1) The
                  overhead of compressing an object under 860 bytes
                  outweighs performance gain. (2) Objects under 860
                  bytes can be transmitted via a single packet anyway,
                  so there isn't a compelling reason to compress them.")

(def long-string (apply str (repeat 860 "a")))

(def long-seq (repeat 860 "a"))

(def long-stream
  (with-open [in (io/input-stream (byte-array (map byte (repeat 860 0))))]
    in))

(defn req
  [& {:keys [server-port server-name remote-addr uri scheme request-method
             headers] :or {server-port 80 server-name "localhost" remote-addr
                           "127.0.0.1" uri "/index" scheme :http request-method
                           :get headers {}}}]
  {:server-port server-port
   :server-name server-name
   :remote-addr remote-addr
   :uri uri
   :scheme scheme
   :request-method request-method
   :headers headers})

(deftest test-wrap-gzip

         (testing "valid-accept-encoding"
                  (doseq [accept-encoding ["gzip" "gzip,deflate"
                                           "gzip,deflate,sdch" "deflate,gzip"
                                           "gzip, deflate" "XXXX" "~~~~"
                                           "-------------"]
                          :let [handler (constantly {:status 200 :headers {}
                                                     :body long-string})
                                req (req :headers {"accept-encoding"
                                                   accept-encoding})
                                resp ((wrap-gzip handler) req)]]
                    (is (true? (instance? java.io.PipedInputStream
                                          (resp :body))))))

         (testing "invalid-accept-encoding"
                  (doseq [accept-encoding ["" "deflate" "deflate,sdch"
                                           " deflate"]
                          :let [handler (constantly {:status 200 :headers {}
                                                     :body long-string})
                                req (req :headers {"accept-encoding"
                                                   accept-encoding})
                                resp ((wrap-gzip handler) req)]]
                    (is (false? (instance? java.io.PipedInputStream
                                           (resp :body))))))

         (testing "response-headers"
                  (let [handler (constantly {:status 200 :headers {}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "Vary"]) "Accept-Encoding"))
                    (is (= (get-in resp [:headers "Content-Encoding"])
                           "gzip"))
                    (is (= (get-in resp [:headers "Content Length"]) nil))))

         (testing "response-headers-with-existing-vary"
                  (let [handler (constantly {:status 200
                                             :headers {"vary" "Accept-Language"}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "Vary"])
                           "Accept-Language, Accept-Encoding")))
                  (let [handler (constantly {:status 200
                                             :headers {"Vary" "Accept-Language"}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "Vary"])
                           "Accept-Language, Accept-Encoding"))))

         (testing "response-headers-with-existing-content-length"
                  (let [handler (constantly {:status 200
                                             :headers {"content-length" (count long-string)}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "content-length"])
                           nil)))
                  (let [handler (constantly {:status 200
                                             :headers {"Content-Length" (count long-string)}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "Content-Length"])
                           nil))))

         (testing "supported-statuses"
                  (doseq [status [200, 201, 202, 203, 204, 205, 403, 404]
                          :let [handler (constantly {:status status :headers {}
                                                     :body long-string})
                                req (req :headers {"accept-encoding" "gzip"})
                                resp ((wrap-gzip handler) req)]]
                    (is (= (get-in resp [:headers "Content-Encoding"])
                           "gzip"))))

         (testing "unsupported-statuses"
                  (doseq [status [206, 301, 302, 304, 305, 307, 400, 401, 502]
                          :let [handler (constantly {:status status :headers {}
                                                     :body long-string})
                                req (req :headers {"accept-encoding" "gzip"})
                                resp ((wrap-gzip handler) req)]]
                    (is (not= (get-in resp [:headers "Content-Encoding"])
                              "gzip"))))

         (testing "response-headers-with-encoded-type"
                  (let [handler (constantly {:status 200
                                             :headers {"content-encoding"
                                                       "deflate"}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "deflate, gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (not= (get-in resp [:headers "Content-Encoding"])
                              "gzip"))))

         (testing "small-response-size"
                  (let [handler (constantly {:status 200
                                             :headers {}
                                             :body short-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (not= (get-in resp [:headers "Content-Encoding"])
                              "gzip"))))

         (testing "large-response-size"
                  (let [handler (constantly {:status 200
                                             :headers {}
                                             :body long-string})
                        req (req :headers {"accept-encoding" "gzip"})
                        resp ((wrap-gzip handler) req)]
                    (is (= (get-in resp [:headers "Content-Encoding"])
                           "gzip"))))

         (testing "response-types"
                  (doseq [body [long-string long-seq long-stream]
                          :let [handler (constantly {:status 200 :headers {}
                                                     :body body})
                                req (req :headers {"accept-encoding" "gzip"})
                                resp ((wrap-gzip handler) req)]]
                    (is (= (get-in resp [:headers "Content-Encoding"])
                           "gzip")))))
