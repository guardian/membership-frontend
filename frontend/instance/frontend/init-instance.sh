#!/bin/bash -ev
# this script is called by all instances when they first start.
# you can test it standalone by exporting the stack/stage/app/region variables and running it manually

CONF_DIR=/etc/frontend
# download dist
mkdir /dist
aws --region $region s3 cp --recursive s3://membership-dist/${stack}/${stage}/frontend/ /dist
# download all private
mkdir /private
aws --region ${region} s3 cp --recursive s3://membership-private/ALLSTAGE/ /private


apt install pwgen
SECRET=`pwgen -s 100 1`
if [ "${#SECRET}" -lt "100" ]; then
 echo pwgen failed, secret was not set, JMX wont be secure
 exit 1
else
 echo ok
fi
echo "collectd $SECRET" >/etc/gu/jmxremote.password
chmod 600 /etc/gu/jmxremote.password
chown frontend /etc/gu/jmxremote.password

# download private for this stage
mkdir /etc/gu
aws --region ${region} s3 cp s3://membership-private/${stage}/membership.private.conf /etc/gu
# enable JMX
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
sed -f /private/liberato.sed /opt/collectd/etc/collectd.conf.d/librato.conf.old >/opt/collectd/etc/collectd.conf.d/librato.conf

# tell collectd how to talk to the JVM
mv /opt/collectd/etc/collectd.conf.d/jvm.conf /opt/collectd/etc/collectd.conf.d/jvm.conf.old
sed -e "s/ServiceURL.*$/\\0\nUser \"collectd\"\nPassword \"$SECRET\"/" /opt/collectd/etc/collectd.conf.d/jvm.conf.old >/opt/collectd/etc/collectd.conf.d/jvm.conf

sudo service collectd restart
