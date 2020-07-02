#!/bin/bash
echo ${BUILD_NUMBER:-DEV} > conf/build.txt

export NVM_DIR="$HOME/.nvm"
[[ -s "$NVM_DIR/nvm.sh" ]] && . "$NVM_DIR/nvm.sh"  # This loads nvm

nvm install
nvm use

# put the init-instance.sh script into the riff-raff package so that it ends up in S3 ready
# to be fetched by the instance on boot
# the special ##teamcity command causes teamcity to write it to the build output
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "##teamcity[publishArtifacts '$DIR/instance => .']"
