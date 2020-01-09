#!/bin/bash
sbt -mem 2048 -Djava.awt.headless=true "project frontend" "devrun"
