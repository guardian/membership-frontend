#!/bin/bash
sbt -Djava.awt.headless=true -jvm-debug 9998 "project frontend" "devrun"
