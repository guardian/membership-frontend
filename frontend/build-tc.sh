#!/bin/bash
echo ${BUILD_NUMBER:-DEV} > conf/build.txt

# put the init-instance.sh script into the riff-raff package so that it ends up in S3 ready
# to be fetched by the instance on boot
# the special ##teamcity command causes teamcity to write it to the build output
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "##teamcity[publishArtifacts '$DIR/instance => .']"
