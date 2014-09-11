#!/bin/bash

wget -O - http://packages.elasticsearch.org/GPG-KEY-elasticsearch | apt-key add -
echo "deb http://packages.elasticsearch.org/logstash/1.4/debian stable main" > /etc/apt/sources.list.d/logstash.list
echo "deb http://packages.elasticsearch.org/elasticsearch/1.3/debian stable main" > /etc/apt/sources.list.d/elasticsearch.list

apt-get -y update
apt-get -y install language-pack-en openjdk-7-jre-headless unzip awscli logstash
