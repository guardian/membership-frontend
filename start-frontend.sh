#!/bin/bash
sbt -Dconfig.resource=dev.conf "project frontend" "run 9100"
