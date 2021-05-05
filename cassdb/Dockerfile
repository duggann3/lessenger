FROM cassandra:3.11
ENV CASSANDRA_KEYSPACE kafka_examples
ENV TABLE chats
COPY connector-config.json /
COPY start_cassdb.sh /
RUN chmod u+x start_cassdb.sh
ENTRYPOINT ["/start_cassdb.sh"]
CMD ["cassandra", "-f"]
