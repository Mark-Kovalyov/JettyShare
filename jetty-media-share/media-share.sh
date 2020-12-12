#!/bin/bash -v

java -jar target/jetty-media-share-*.jar \
 --host localhost \
 --port 8082 \
 --root /storage
