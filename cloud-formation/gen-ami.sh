#!/bin/bash

apt-get -y update
apt-get -y upgrade
apt-get -y install language-pack-en openjdk-7-jre-headless unzip awscli python-pip

# used by AWS log agent
pip install virtualenv