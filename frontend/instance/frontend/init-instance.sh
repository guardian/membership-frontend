#!/bin/bash -ev
# this script is called by all instances when they first start.
# you can test it standalone by exporting the stack/stage/app/region variables and running it manually

# download dist
CONF_DIR=/etc/frontend
mkdir /dist
aws --region $region s3 cp --recursive s3://membership-dist/${stack}/${stage}/frontend/ /dist
# get secrets
mkdir /etc/gu
aws --region ${region} s3 cp s3://membership-private/${stage}/membership.private.conf /etc/gu
# install play app
dpkg -i /dist/frontend_1.0-SNAPSHOT_all.deb
# permissions on the secrets
chown frontend /etc/gu/membership.private.conf
chmod 0600 /etc/gu/membership.private.conf
# enable cloudwatch log uploader
/opt/cloudwatch-logs/configure-logs application ${stack} ${stage} ${app} /var/log/frontend/frontend.log
# enable liberato jvm gather agent
#see https://www.librato.com/docs/kb/collect/integrations/agent/index.html
# use the advanced method otherwise it complains the key has expired
mkdir -p /etc/apt/sources.list.d/
cp /dist/librato_librato-collectd.list /etc/apt/sources.list.d/

sudo apt-get install debian-archive-keyring
sudo apt-get install apt-transport-https
curl https://packagecloud.io/gpg.key 2> /dev/null | sudo apt-key add -

cp /dist/librato-collectd /etc/apt/preferences.d/
sudo apt-get update
sudo apt-get -y install collectd

aws --region ${region} s3 cp s3://membership-private/liberato.sed /tmp/
mv /opt/collectd/etc/collectd.conf.d/librato.conf /opt/collectd/etc/collectd.conf.d/librato.conf.old
sed -f /tmp/liberato.sed /opt/collectd/etc/collectd.conf.d/librato.conf.old >/opt/collectd/etc/collectd.conf.d/librato.conf

sudo service collectd restart
