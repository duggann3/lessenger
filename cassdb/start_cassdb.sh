#!/bin/bash
if [[ ! -z "$CASSANDRA_KEYSPACE" && $1 = 'cassandra' ]]; then
  # Create default keyspace for single node cluster
  CQL="CREATE KEYSPACE $CASSANDRA_KEYSPACE WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1}; USE $CASSANDRA_KEYSPACE; CREATE TABLE $TABLE(sender text, message text, date_time text, channel text,PRIMARY KEY (date_time));"
    while true; do
        if echo $CQL | cqlsh; then
            echo "sleeping for 1 minute until datastack-connector comes up"
            sleep 1m
            echo "Starting the datastax connector"
            curl \
            -X POST \
            -H "Content-Type: application/json" \
            -d @connector-config.json "http://datastax-connect:8083/connectors"
            break
        else
            echo "cqlsh: Cassandra is unavailable - retry later"
            sleep 2
        fi
    done &
fi

exec /docker-entrypoint.sh "$@"
