#!/bin/bash -ev
# this script is called by all instances when they first start.
# you can test it standalone by exporting the stack/stage/app/region variables and running it manually

# download play app
CONF_DIR=/etc/frontend
aws --region $region s3 cp s3://membership-dist/${stack}/${stage}/frontend/frontend_1.0-SNAPSHOT_all.deb /tmp
# get secrets
mkdir /etc/gu
aws --region ${region} s3 cp s3://membership-private/${stage}/membership.private.conf /etc/gu
# install play app
dpkg -i /tmp/frontend_1.0-SNAPSHOT_all.deb
# permissions on the secrets
chown frontend /etc/gu/membership.private.conf
chmod 0600 /etc/gu/membership.private.conf
# enable cloudwatch log uploader
/opt/cloudwatch-logs/configure-logs application ${stack} ${stage} ${app} /var/log/frontend/frontend.log
# enable liberato jvm gather agent
curl -s https://metrics-api.librato.com/agent_installer/bd15a274590a10f2 | sudo bash
