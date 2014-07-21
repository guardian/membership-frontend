#!/usr/bin/env bash

set -e

[ -d target ] && rm -rf target
mkdir target
cd target
mkdir -p packages/keys

for USER in $(cat ../users.txt)
do
    (curl "https://github.com/$USER.keys" && echo -e "\n") >> packages/keys/authorized_keys
done

cp ../deploy.json .

zip -rv artifacts.zip packages/ deploy.json

echo "##teamcity[publishArtifacts '$(pwd)/artifacts.zip => .']"