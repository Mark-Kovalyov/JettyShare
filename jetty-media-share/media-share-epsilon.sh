#!/bin/bash -v

java \
 -XX:+UnlockExperimentalVMOptions \
 -XX:+UseEpsilonGC \
 -jar target/jetty-media-share-*.jar \
   --host localhost \
   --port 8082 \
   --root /storage
