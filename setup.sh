#!/bin/bash

#####################################################
#
# Set up the Membership application
#
#####################################################

#####################################################
# Install NPM modules
#####################################################

printf "Installing NPM modules ...\n\r\n\r"

npm install

#####################################################
# Install Bower modules
#####################################################

printf "Installing bower modules in common/app/assets/javascripts ...\n\r\n\r"

cd common/app/assets/javascripts

bower install

cd ../../../../

#####################################################
# Compile clientside assets
#####################################################

printf "Compiling assets ...\n\r\n\r"

grunt compile

printf "\n\r\n\rGood to go -> 'play run' to start app\n\r\n\r"