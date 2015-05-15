#!/bin/bash -e

#####################################################
# Set up the Membership application
#####################################################

# fix the issues with bower installs
git config --local url."https://".insteadOf git://

cd frontend

#####################################################
# Bundle
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing Ruby gems..."
printf "\n\r\n\r====================================\n\r\n\r"

if hash bundler 2>/dev/null; then
    bundle install
else
    printf "\nYou need to install bundler first:\n"
    printf "\ngem install bundler. http://bundler.io/\n"
    exit 1
fi

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
printf "> Installing bower JS modules in assets/javascripts ..."
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
printf "> Installing bower SASS modules in assets/stylesheets ..."
printf "\n\r\n\r====================================\n\r\n\r"

pushd assets/stylesheets
rm -rf components/bower-components
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
