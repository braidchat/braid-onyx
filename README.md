# braid-onyx

An work-in-progress Onyx application to do useful things for Braid.
Currently, its purpose is to monitor the Datomic log and send messages content to ElasticSearch to facilitate better search for Braid.

This is still a work-in-progress, instructions on use will be added when things are actually working.

# Setting Up

## Elasticsearch

The following instructions assume Ubuntu 16.04

 - [install java](https://www.digitalocean.com/community/tutorials/how-to-install-java-on-ubuntu-with-apt-get)

 - install elasticsearch 2.x

    ```
    wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | sudo apt-key add -
    echo "deb https://packages.elastic.co/elasticsearch/2.x/debian stable main" | sudo tee -a /etc/apt/sources.list.d/elasticsearch-2.x.list
    sudo apt-get update
    sudo apt-get install elasticsearch
    ```

 - optionally, edit `/etc/elasticsearch/elasticsearch.yml` (e.g. in prod, probably should change `cluster.name`)

 - start elasticsearch running

    ```
    sudo systemctl enable elasticsearch.service
    ```

## Running Onyx

<!-- TODO:  -->
    lein run start-peers 10
    lein run submit-job datomic-job

# Prod Deployments

## Elasticsearch in Prod

If elasticsearch is running on a different host, you'll need to set up the
firewall to allow that.

  1. Make elasticsearch bind to the network interface by setting `http.host: _eth0_` in elasticsearch.yml
  2. Allow connections from the braid app server: `sudo ufw allow from $BRAID_IP to any port 9200`

## Connecting to prod datomic

If the datomic is running on another machine, you'll need to make sure the
interface is available.  If Datomic is using Postgresql as the backing store,
you'll need to do the following:

  1. Edit `/etc/postgresql/9.5/main/postgresql.conf` and change `listen_address` from `localhost` to `*`
  2. Allow the datomic user to connect over TCP: Edit `/etc/postgresql/9.5/main/pg_hba.conf` and add the following line: `
host    datomic         all             159.203.33.218/32          md5`
  3. Allow connections from the onyx server to postgres: `sudo ufw allow from $ONYX_IP to any port 5432`
  4. Edit the datomic config file to set `host` to `0.0.0.0` and `alt-host` to the public IP of the braid server
  5. Allow connections from the onyx server to datomic: `sudo ufw allow from $ONYX_IP to any port 4334`
