#!/usr/bin/env bash

curl -I http://localhost:8082/

curl -I -H "If-None-Match : '12345'" http://localhost:8082/
