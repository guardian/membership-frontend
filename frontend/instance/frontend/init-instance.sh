#!/bin/bash -ev
# this script is called by all instances when they first start.
# you can test it standalone by exporting the stack/stage/app/region variables and running it manually

CONF_DIR=/etc/frontend
# download dist
mkdir /dist
aws --region $region s3 cp --recursive s3://membership-dist/${stack}/${stage}/frontend/ /dist
# download all private
mkdir /private
aws --region ${region} s3 cp s3://membership-private/ALLSTAGE/liberato.sed /private
# download private for this stage
mkdir /etc/gu
aws --region ${region} s3 cp s3://membership-private/${stage}/membership.private.conf /etc/gu

# generate password for JMX
apt install pwgen
SECRET=`pwgen -s 100 1`
if [ "${#SECRET}" -lt "100" ]; then
 echo pwgen failed, secret was not set, JMX wont be secure
 exit 1
else
 echo ok
fi
echo "collectd $SECRET" >/etc/gu/jmxremote.password
echo "collectd readonly" >/etc/gu/jmxremote.access

# install and start play app and create frontend user
dpkg -i /dist/frontend_1.0-SNAPSHOT_all.deb

# permissions on the secrets now the frontend user will exist
chown frontend /etc/gu/membership.private.conf
chmod 0600 /etc/gu/membership.private.conf
chmod 600 /etc/gu/jmxremote.password
chown frontend /etc/gu/jmxremote.password

# enable cloudwatch log uploader
/opt/cloudwatch-logs/configure-logs application ${stack} ${stage} ${app} /var/log/frontend/frontend.log

# install liberato jvm gather agent
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

mv /opt/collectd/etc/collectd.conf.d/librato.conf /opt/collectd/etc/collectd.conf.d/librato.conf.old
cat /opt/collectd/etc/collectd.conf.d/librato.conf.old | \
  sed -e "s/</Node>/LogHttpError true\nStoreRates true\n</Node>" - | \
  sed -f /private/liberato.sed - \
  >/opt/collectd/etc/collectd.conf.d/librato.conf

cd /opt/collectd/etc/collectd.conf.d/
rm apache.conf haproxy.conf mysql.conf nginx* postgresql.conf zookeeper.conf redis.conf varnish.conf mongodb.conf memcached.conf elasticsearch.conf docker.conf
cd -

# tell collectd how to talk to the JVM JMX interface
mv /opt/collectd/etc/collectd.conf.d/jvm.conf /opt/collectd/etc/collectd.conf.d/jvm.conf.old
cat /opt/collectd/etc/collectd.conf.d/jvm.conf.old | \
  sed -e "s/ServiceURL.*$/\\0\nUser \"collectd\"\nPassword \"$SECRET\"/" - | \
  sed -e "s/Host \"localhost\"//" - \
  >/opt/collectd/etc/collectd.conf.d/jvm.conf2

HOST=`curl http://169.254.169.254/latest/meta-data/instance-id`

#turn on collectd logging
mv /opt/collectd/etc/collectd.conf /opt/collectd/etc/collectd.conf.old
cat /opt/collectd/etc/collectd.conf.old | \
  sed -e "s/Hostname    \"localhost\/Hostname \"$HOST\""
  sed -e 's/#LoadPlugin syslog/LoadPlugin syslog/' - | \
  >/opt/collectd/etc/collectd.conf
sudo service collectd restart

echo "finished instance init script (hopefully it worked ...due to not much error handling)"
