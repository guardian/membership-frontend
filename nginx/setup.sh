#!/bin/bash -x

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

echo "\n\nUsing NGINX_HOME=$NGINX_HOME"

sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/membership.conf $NGINX_HOME/sites-enabled/membership.conf
