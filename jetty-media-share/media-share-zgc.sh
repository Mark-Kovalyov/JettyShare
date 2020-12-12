#!/bin/bash -v

java \
 -XX:+UnlockExperimentalVMOptions \
 -XX:+UseZGC \
 -jar target/jetty-media-share-*.jar \
   --host localhost \
   --port 8082 \
   --root /storage
