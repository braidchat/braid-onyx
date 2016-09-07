(ns braid-onyx.jobs.datomic
  (:require
    [datomic.api :as d]
    [onyx.job :refer [add-task register-job]]
    [onyx.plugin.datomic]
    [onyx.plugin.elasticsearch]
    [onyx.tasks.datomic :as datomic-task]))

;; Workflow

(def workflow
  [[:read-log :split-txns]
   [:split-txns :process-for-es]
   [:process-for-es :write-messages]])

;; Catalog entries

(defn build-catalog
  ([] (build-catalog 5 50))
  ([batch-size batch-timeout]
   [{:onyx/name :write-messages
     :onyx/plugin :onyx.plugin.elasticsearch/write-messages
     :onyx/type :output
     :onyx/medium :elasticsearch
     :elasticsearch/host "127.0.0.1"
     :elasticsearch/port 9200
     ;:elasticsearch/cluster-name "my-cluster-name"
     :elasticsearch/client-type :http
     ;:elasticsearch/http-ops {:basic-auth ["user" "pass"]}
     :elasticsearch/index "braid-messages"
     :elasticsearch/mapping "messages-mapping"
     ;:elasticsearch/doc-id "my-id"
     :elasticsearch/write-type :insert
     :onyx/batch-size batch-size
     :onyx/doc "Writes documents to elasticsearch"}

    {:onyx/name :split-txns
     :onyx/fn ::split-txns
     :onyx/type :function
     :onyx/batch-size batch-size
     :onyx/batch-timeout batch-timeout}

    {:onyx/name :process-for-es
     :onyx/fn ::process-for-es
     :onyx/type :function
     :onyx/batch-size batch-size
     :onyx/batch-timeout batch-timeout}]))

(defn process-for-es
  [{[eid attr v t insert?] :txn :as segment}]
  {:elasticsearch/message {:content v}
   :elasticsearch/doc-id (str eid)})

(defn split-txns
  [{:keys [id data t] :as segment}]
  (map (fn [d] {:txn d}) data))

;; Lifecycles

(defn build-lifecycles
  []
  [{:lifecycle/task :read-log
    :lifecycle/calls :onyx.plugin.datomic/read-log-calls}

   {:lifecycle/task :write-messages
    :lifecycle/calls :onyx.plugin.elasticsearch/write-messages-calls}])

;; flow conditions

(def attribute-id
  (memoize (fn [db-uri attr]
             (-> (d/pull (d/db (d/connect db-uri)) [:db/id] [:db/ident attr])
                 :db/id))))

(defn message?
  [event {[eid attr v t insert?] :txn :as old-segment} new-segment all-new-segments db-uri]
  (and insert? (= (attribute-id db-uri :message/content) attr)))

(defn build-flow-conditions
  [db-uri]
  [{:flow/from :process-for-es
    :flow/to [:write-messages]
    ::db-uri db-uri
    :flow/predicate [::message? ::db-uri]}])

;; the job, proper

(defn datomic-job
  [{:keys [onyx/batch-size onyx/batch-timeout] :as batch-settings} db-uri]
  (let [job {:workflow workflow
             :catalog (build-catalog batch-size batch-timeout)
             :lifecycles (build-lifecycles)
             :windows []
             :triggers []
             :flow-conditions (build-flow-conditions db-uri)
             :task-scheduler :onyx.task-scheduler/balanced}]
    (-> job
        (add-task (datomic-task/read-log :read-log
                               (merge {:datomic/uri db-uri
                                       :checkpoint/key "checkpoint"
                                       :checkpoint/force-reset? false
                                       :onyx/max-peers 1}
                                      batch-settings))))))

(defmethod register-job "datomic-job"
  [job-name {db-uri :datomic/db-uri :as config}]
  (let [batch-settings {:onyx/batch-size 1 :onyx/batch-timeout 1000}]
    (datomic-job batch-settings db-uri)))
