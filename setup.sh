#!/bin/bash

#####################################################
#
# Set up the Membership application
#
#####################################################

echo " ____ ____ ____ ____ ____ ____ ____ ____ ____ ____ ";
echo "||m |||e |||m |||b |||e |||r |||s |||h |||i |||p ||";
echo "||__|||__|||__|||__|||__|||__|||__|||__|||__|||__||";
echo "|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|";

printf "\n\r"

#####################################################
# Install NPM modules
#####################################################

printf "> Installing NPM modules ...\n\r\n\r"

npm install &> /dev/null

#####################################################
# Install Bower JS modules
#####################################################

printf "> Installing bower JS modules in common/app/assets/javascripts ...\n\r\n\r"

cd common/app/assets/javascripts

bower install &> /dev/null

cd ../../../../

#####################################################
# Install Bower SASS modules
#####################################################

printf "> Installing bower SASS modules in common/app/assets/stylesheets ...\n\r\n\r"

cd common/app/assets/stylesheets

bower install &> /dev/null

cd ../../../../

#####################################################
# Compile clientside assets
#####################################################

printf "> Compiling assets ...\n\r\n\r"

grunt compile &> /dev/null

#####################################################
# Add commit hook
#####################################################

printf "> Adding git commit hook ...\n\r\n\r"

grunt hookup &> /dev/null

#####################################################
# Done
#####################################################

printf "> Good to go -> 'play run' to start app\n\r\n\r"