#!/bin/bash
echo ${BUILD_NUMBER:-DEV} > conf/build.txt
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "##teamcity[publishArtifacts '$DIR/init-instance.sh => .']"
