#!/bin/bash
echo ${BUILD_NUMBER:-DEV} > conf/build.txt
# put the script needed on the machine into the package so it ends up on the instance via riffraff
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "##teamcity[publishArtifacts '$DIR/instance => .']"
