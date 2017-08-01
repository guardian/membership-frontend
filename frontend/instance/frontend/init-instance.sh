#!/bin/bash -v
# this script is called by all instances when they first start.
# you can test it standalone by exporting the stack/stage/app/region variables and running it manually

CONF_DIR=/etc/frontend
# download dist
mkdir /dist
aws --region $region s3 cp --recursive s3://membership-dist/${stack}/${stage}/frontend/ /dist
# download private for this stage
mkdir /etc/gu
aws --region $region s3 cp s3://membership-private/${stage}/membership.private.conf /etc/gu

# wait for dpkg lock to become free
lsof /var/lib/dpkg/lock
rc=$?
echo "Return code is $rc"
until [ "$rc" -eq "1" ]; do
  echo "Waiting for dpkg lock to become free"
  sleep 1s;
  lsof /var/lib/dpkg/lock
  rc=$?
  echo "Return code is $rc"
done
echo "dpkg lock is free, ready to install application"

# install and start play app and create frontend user
dpkg -i /dist/frontend_1.0-SNAPSHOT_all.deb

# permissions on the secrets now the frontend user will exist
chown frontend /etc/gu/membership.private.conf
chmod 0600 /etc/gu/membership.private.conf

# enable cloudwatch log uploader
/opt/cloudwatch-logs/configure-logs application ${stack} ${stage} ${app} /var/log/frontend/frontend.log

echo "finished instance init script (hopefully it worked ...due to not much error handling)"
