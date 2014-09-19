#!/bin/bash -e

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

echo "  ^    ^    ^    ^    ^    ^    ^    ^  "
echo " /f\  /r\  /o\  /n\  /t\  /e\  /n\  /d\ "
echo "<___><___><___><___><___><___><___><___>"

cd frontend

#####################################################
# Install NPM modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing NPM modules ..."
printf "\n\r\n\r====================================\n\r\n\r"

npm install

#####################################################
# Install Bower JS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing bower JS modules in assets/javascripts ..."
printf "\n\r\n\r====================================\n\r\n\r"

pushd assets/javascripts
if [ -d "lib/bower-components" ]; then
    printf "\n\r\n\r====================================\n\r\n\r"
    printf "> Removing JavaScripts Bower Components folder ..."
    printf "\n\r\n\r====================================\n\r\n\r"
    rm -rf lib/bower-components
fi

bower install

popd

#####################################################
# Install Bower SASS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing bower SASS modules in assets/stylesheets ..."
printf "\n\r\n\r====================================\n\r\n\r"

pushd assets/stylesheets
if [ -d "components/bower-components" ]; then
    printf "\n\r\n\r====================================\n\r\n\r"
    printf "> Removing Stylesheets Bower Components folder ..."
    printf "\n\r\n\r====================================\n\r\n\r"
    rm -rf components/bower-components
fi

bower install

popd

#####################################################
# Compile clientside assets
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Compiling assets ..."
printf "\n\r\n\r====================================\n\r\n\r"

grunt compile

#####################################################
# Add commit hook
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Adding git commit hook ..."
printf "\n\r\n\r====================================\n\r\n\r"

grunt hookup

#####################################################
# Done
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Good to go.\n\r\n\r"
