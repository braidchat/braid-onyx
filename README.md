# braid-onyx

An work-in-progress Onyx application to do useful things for Braid.
Currently, its purpose is to monitor the Datomic log and send messages content to ElasticSearch to facilitate better search for Braid.

This is still a work-in-progress, instructions on use will be added when things are actually working.

# Setting Up

## Elasticsearch

The following instructions assume Ubuntu 16.04

 - install elasticsearch 2.x

    wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | sudo apt-key add -
    echo "deb https://packages.elastic.co/elasticsearch/2.x/debian stable main" | sudo tee -a /etc/apt/sources.list.d/elasticsearch-2.x.list
    sudo apt-get update
    sudo apt-get install elasticsearch

 - optionally, edit `/etc/elasticsearch/elasticsearch.yml` (e.g. in prod, probably should change `cluster.name`)

 <!-- at some point, I needed to change a config file to make the daemon start from the service - not the case with the 2.x version? -->
 - start elasticsearch running

    sudo systemctl enable elasticsearch.service

## Running Onyx

<!-- TODO:  -->
    lein run start-peers 10
    lein run submit-job datomic-job
