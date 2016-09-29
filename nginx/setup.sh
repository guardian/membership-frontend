#!/bin/bash -x

GU_KEYS="${HOME}/.gu/keys"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/membership.conf $NGINX_HOME/sites-enabled/membership.conf
sudo ln -fs $DIR/membership.crt $NGINX_HOME/membership.crt
sudo ln -fs $DIR/membership.key $NGINX_HOME/membership.key

aws s3 cp s3://identity-local-ssl/idapi-code-proxy-thegulocal-com-exp2017-03-31-bundle.crt ${GU_KEYS}/ --profile membership
aws s3 cp s3://identity-local-ssl/idapi-code-proxy-thegulocal-com-exp2017-03-31.key ${GU_KEYS}/ --profile membership
aws s3 cp s3://identity-local-ssl/mem-thegulocal-com-exp2017-03-31-bundle.crt ${GU_KEYS}/ --profile membership
aws s3 cp s3://identity-local-ssl/mem-thegulocal-com-exp2017-03-31.key ${GU_KEYS}/ --profile membership

sudo ln -fs ${GU_KEYS}/ $NGINX_HOME/keys

sudo nginx -s stop
sudo nginx

