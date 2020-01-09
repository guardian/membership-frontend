#!/bin/bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

dev-nginx setup-cert mem.thegulocal.com
dev-nginx link-config $DIR/membership.conf
dev-nginx restart-nginx
