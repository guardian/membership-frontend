#!/bin/bash
echo ${BUILD_NUMBER:-DEV} > conf/build.txt

# put the script needed on the machine into the package so it ends up on the instance via riffraff
# the special ##teamcity command causes teamcity to write it to the build output
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "##teamcity[publishArtifacts '$DIR/instance => .']"
