(ns braid-onyx.jobs.datomic
  (:require [onyx.job :refer [add-task register-job]]
            [onyx.tasks.datomic :as datomic-task]
            [onyx.plugin.datomic]
            [onyx.plugin.http-output]
            [onyx.plugin.elasticsearch]
            [datomic.api :as d]))

(def db-uri
  "TODO"
  "datomic:dev://localhost:4334/chat-dev")

(def workflow
  [;[:read-log :write-messages]
   [:read-log :split-txns]
   [:split-txns :process-for-req]
   [:process-for-req :http-output]
   ])

(defn build-catalog
  ([] (build-catalog 5 50))
  ([batch-size batch-timeout]
   [{:onyx/name :write-messages
     :onyx/plugin :onyx.plugin.elasticsearch/write-messages
     :onyx/type :output
     :onyx/medium :elasticsearch
     :elasticsearch/host "127.0.0.1"
     :elasticsearch/port 9200
     :elasticsearch/cluster-name "my-cluster-name"
     :elasticsearch/client-type :http
     :elasticsearch/http-ops {:basic-auth ["user" "pass"]}
     :elasticsearch/index "my-index-name"
     :elasticsearch/mapping "my-mapping-name"
     :elasticsearch/doc-id "my-id"
     :elasticsearch/write-type :insert
     :onyx/batch-size batch-size
     :onyx/doc "Writes documents to elasticsearch"}

    {:onyx/name :http-output
     :onyx/plugin :onyx.plugin.http-output/output
     :onyx/type :output
     :onyx/medium :http
     :http-output/success-fn ::success?
     :onyx/batch-size batch-size
     :onyx/doc "POST segments to http endpoint"}

    {:onyx/name :split-txns
     :onyx/fn ::split-txns
     :onyx/type :function
     :onyx/batch-size batch-size
     :onyx/batch-timeout batch-timeout}

    {:onyx/name :process-for-req
     :onyx/fn ::process-for-req
     :onyx/type :function
     :onyx/batch-size batch-size
     :onyx/batch-timeout batch-timeout}

    ]))

(def success? (constantly true))

(defn process-for-req
  [segment]
  {:url "http://localhost:9999/" :args {:body (pr-str segment)}})

(defn split-txns
  [{:keys [id data t] :as segment}]
  (map (fn [d] {:txn d}) data))

(defn build-lifecycles
  []
  [{:lifecycle/task :read-log
    :lifecycle/calls :onyx.plugin.datomic/read-log-calls}

   {:lifecycle/task :write-messages
    :lifecycle/calls :onyx.plugin.elasticsearch/write-messages-calls}])

(def message-content-id
  (:db/id (d/pull (d/db (d/connect db-uri)) [:db/id] [:db/ident :message/content])))

(defn message?
  [event {[eid attr v t insert?] :txn :as old-segment} new-segment all-new-segments]
  (println "checking message " old-segment)
  (and insert? (= message-content-id attr)))

(def flow-conditions
  [{:flow/from :process-for-req
    :flow/to [:http-output]
    :flow/predicate ::message?}])

(defn datomic-job
  [{:keys [onyx/batch-size onyx/batch-timeout] :as batch-settings}]
  (let [job {:workflow workflow
             :catalog (build-catalog batch-size batch-timeout)
             :lifecycles (build-lifecycles)
             :windows []
             :triggers []
             :flow-conditions flow-conditions
             :task-scheduler :onyx.task-scheduler/balanced}]
    (-> job
        (add-task (datomic-task/read-log :read-log
                               (merge {:datomic/uri db-uri
                                       :checkpoint/key "checkpoint"
                                       :checkpoint/force-reset? false
                                       :onyx/max-peers 1}
                                      batch-settings))))))

(defmethod register-job "datomic-job"
  [job-name config]
  (let [batch-settings {:onyx/batch-size 1 :onyx/batch-timeout 1000}]
    (datomic-job batch-settings)))
