#!/bin/bash -v

java \
 -XX:+UnlockExperimentalVMOptions \
 -XX:+UseShenanodoahGC \
   -XX:ShenandoahGCHeuristics=Adaptive \
 -jar target/jetty-media-share-*.jar \
   --host localhost \
   --port 8082 \
   --root /storage
