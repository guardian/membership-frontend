#!/bin/bash

printf "Installing NPM modules ...\n\r\n\r"

npm install

cd common/app/assets/javascripts

printf "Installing bower modules in common/app/assets/javascripts ...\n\r\n\r"

bower install

cd ../../../../

printf "Compiling assets ...\n\r\n\r"

grunt compile

printf "\n\r\n\rGood to go -> 'play run' to start app\n\r\n\r"