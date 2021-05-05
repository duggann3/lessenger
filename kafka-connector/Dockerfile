FROM confluentinc/cp-kafka-connect-base:5.3.0
ARG CONNECTOR=kafka-connect-cassandra-sink-1.4.0.tar.gz
RUN mkdir /datastax-connector
COPY ${CONNECTOR} /datastax-connector/
RUN cd /datastax-connector && tar xzf ${CONNECTOR} --strip-components=1 \
&& mv kafka-connect*.jar kafka-connect-cassandra.jar
ENV CONNECT_PLUGIN_PATH="/usr/share/java,/datastax-connector/kafka-connect-cassandra.jar"
