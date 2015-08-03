#!/bin/bash
set -e

installed() {
    hash "$1" 2>/dev/null
}

banner() {
    printf "\n\r\n\r====================================\n\r"
    printf "${1}"
    printf "\n\r====================================\n\r\n\r"
}

banner "Guardian Membership"

install_grunt() {
    if installed grunt && installed npm; then
        npm install
    else
        EXTRA_STEPS+=("You need to install node to continue https://nodejs.org/")
        EXTRA_STEPS+=("You need to install grunt-cli first. npm install -g grunt-cli")
    fi
}

install_bower() {
    if installed bower; then

        banner "Installing Bower JS modules"
        pushd assets/javascripts
        rm -rf lib/bower-components
        bower install
        popd

        banner "Installing Bower CSS modules"
        pushd assets/stylesheets
        rm -rf components/bower-components
        bower install
        popd
    else
        EXTRA_STEPS+=("You need to install bower first. npm install -g bower")
    fi
}

install_dependencies() {
    install_grunt
    install_bower
}

compile_assets() {
    banner "Compiling assets"
    grunt compile
    banner "Adding git commit hooks"
    grunt hookup
}

report() {
    if [[ ${#EXTRA_STEPS[@]} -gt 0 ]]; then
        echo -e
        echo "Remaining tasks: "
        for i in "${!EXTRA_STEPS[@]}"; do
            echo "  $((i+1)). ${EXTRA_STEPS[$i]}"
        done
    fi
}

main() {
    install_dependencies
    compile_assets
    report
}

# fix the issues with bower installs
git config --local url."https://".insteadOf git://

cd frontend

main
