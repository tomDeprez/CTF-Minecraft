#!/bin/bash
while inotifywait -r -e modify,create,delete src; do
    mvn -o clean package
done
