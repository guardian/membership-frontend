#!/bin/bash -e

#####################################################
# Set up the Membership application
#####################################################

echo " ____ ____ ____ ____ ____ ____ ____ ____ ____ ____ ";
echo "||m |||e |||m |||b |||e |||r |||s |||h |||i |||p ||";
echo "||__|||__|||__|||__|||__|||__|||__|||__|||__|||__||";
echo "|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|";

# fix the issues with bower installs
git config --local url."https://".insteadOf git://

cd frontend

#####################################################
# Install NPM modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing NPM modules ..."
printf "\n\r\n\r====================================\n\r\n\r"

if hash grunt 2>/dev/null; then
    npm install
else
    printf "\nYou need to install grunt-cli first:\n"
    printf "\nnpm install -g grunt-cli\n"
    exit 1
fi

#####################################################
# Install Bower JS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing Bower JS modules..."
printf "\n\r\n\r====================================\n\r\n\r"

if hash bower 2>/dev/null; then
    pushd assets/javascripts
    rm -rf lib/bower-components
    bower install
else
    printf "\nYou need to install bower first:\n"
    printf "\nnpm install -g bower\n"
    exit 1
fi

popd

#####################################################
# Install Bower SASS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing Bower SASS modules..."
printf "\n\r\n\r====================================\n\r\n\r"

if hash bower 2>/dev/null; then
    pushd assets/stylesheets
    rm -rf components/bower-components
    bower install
else
    printf "\nYou need to install bower first:\n"
    printf "\nnpm install -g bower\n"
    exit 1
fi

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
