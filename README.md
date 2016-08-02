# Membership Frontend

[https://membership.theguardian.com/](https://membership.theguardian.com/). Bringing the Guardian to life through live events and meet-ups.

## Table of Contents

1. [Getting Started](#getting-started)
1. [Setup](#setup)
1. [Run](#run)
1. [Tests](#tests)
1. [Deployment](#deployment)
1. [Test Users](#test-users)
1. [Security](#security)
1. [Additional Documentation](#additional)

## Getting Started

To get started working on Membership you will need to complete the following steps:

1. Work through the [Setup](#setup) instructions for this project
2. Work through the setup instructions for the [identity frontend](https://github.com/guardian/identity-frontend). This provides a frontend for sign-up and registration.
3. **Optional:** If you need to have a local instance of the Identity API, follow the instructions in [Running Identity API locally](#running-identity-api-locally)
3. Start up Membership by running the commands in the [Run](#run) section of this README

## Setup

### Requirements

We require the following to be installed:

- `Java 8`
- `Node (with npm)`
- `NGINX`

### Install client-side dependencies

```
cd frontend/
npm install
npm run devSetup
```

A good way to check everything is setup correctly is to run the tests

```
npm test
```

For development you'll also need the following commands:

**Compile assets**

```
npm run compile
```

**Watch files for changes**

```
npm run watch
```

*Note:* We use `grunt` and `bower` behind the scenes but provide [facades for common tasks](https://bocoup.com/weblog/a-facade-for-tooling-with-npm-scripts/) to make setup easier and reduce the number of tools needed for most developers. If you want to work with the full set of build tools install `grunt-cli` and run `grunt --help` to see the list of available tasks.

**Client-side Principles**: See [client-side-principles.md](docs/client-side-principles.md) for high-level client-side principles for Membership.

**Pattern Library**: A library of common patterns used across the membership site is available at [membership.theguardian.com/patterns](https://membership.theguardian.com/patterns).

### Setup NGINX

Follow the instructions in [`/nginx/README.md`](./nginx/README.md) in this project.

### Setup AWS credentials

Install the awscli:
```
brew install awscli
```

Fetch the developer AWS credentials, discuss how we do this with a team member.


### Download private keys

Download our private keys from the `membership-private` S3 bucket. If you have the AWS CLI set up you can run:

```
sudo aws s3 cp s3://membership-private/DEV/membership-keys.conf /etc/gu/ --profile membership
```

### Ubuntu setup

In an ideal world, your Ubuntu package install would be:

```
sudo apt-get install nginx openjdk-8-jdk nodejs npm
```

#### [Node](http://nodejs.org/) & [NPM](https://github.com/npm/npm/releases)

See Joyent's instructions on [installing Node & NPM on Ubuntu](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os).
If the available [nodejs](http://packages.ubuntu.com/trusty/nodejs) or [NPM](http://packages.ubuntu.com/trusty/npm)
package for your version of Ubuntu is old, you'll [probably](http://askubuntu.com/questions/49390/how-do-i-install-the-latest-version-of-node-js)
want to install [chris-lea's PPA](https://launchpad.net/~chris-lea/+archive/node.js),
which includes both Node.js and NPM.

## Run

Start the app as follows:

```
./start-frontend.sh
```

This will start the Play application, which usually listens on port `9100`. Making a request to `localhost:9100` should give you the homepage.

To make the site reachable as `mem.thegulocal.com` (necessary for register/sign-in functionality) you then need to make sure NGINX is configured and running as described in [`/nginx/README.md`](./nginx/README.md).

## Testing

### Manual

[See here](https://sites.google.com/a/guardian.co.uk/guardan-identity/identity/test-users) for details of how we do test users.
Note that we read the shared secret for these from the `identity.test.users.secret` property in `membership-keys.conf`.

### Automated

#### JavaScript unit tests

```
cd frontend/
npm test
```


#### Scala unit tests

`sbt fast-test`

#### Acceptance tests

1. Run local membership-frontend: `sbt devrun`
2. Run local [idenity-frontend](https://github.com/guardian/identity-frontend): `sbt devrun`
3. `sbt acceptance-test`

These are browser driving Selenium tests.

#### All tests

`sbt test`


## Deployment

We use continuous deployment of the `master` branch to Production (https://membership.theguardian.com/).
See [fix-a-failed-deploy.md](https://github.com/guardian/deploy/blob/master/magenta-lib/docs/magenta-lib/howto/fix-a-failed-deploy.md)
for what to do if a deploy goes bad.

## Security

### Committing config credentials

For the Membership project, we put both `DEV` and `PROD` credentials in `membership-keys.conf` files in the private S3 bucket `membership-private`, and if private credentials need adding or updating, they need to be updated there in S3.

You can download and update credentials like this

    aws s3 cp s3://membership-private/DEV/membership-keys.conf /etc/gu
    aws s3 cp /etc/gu/membership-keys.conf s3://membership-private/DEV/

For a reminder on why we do this, here's @tackley on the subject:

>NEVER commit access keys, passwords or other sensitive credentials to any source code repository*.

>That's especially true for public repos, but also applies to any private repos. (Why? 1. it's easy to make it public and 2. every person who ever worked on your project almost certainly has a copy of your repo somewhere. It's too easy for a subsequently-disaffected individual to take advantage of this.)

>For AWS access keys, always prefer to use instance profiles instead.

>For other credentials, either use websys's puppet based config distribution (for websys managed machines) or put them in a configuration store such as DynamoDB or a private S3 bucket.

<a name="additional"></a>

## Running Identity API locally

By default the Identity frontend in theguardian.com (which handles sign-in and registration) will talk to a staging instance of the Identity API called `CODE`. If you need a local instance of the Identity API, follow the instructions below.

**Identity repo**: [https://github.com/guardian/identity-frontend](https://github.com/guardian/identity-frontend)

Run through the set up instructions; once complete you will need to run:

```
 nginx/setup.sh
 ./start-api.sh
```

Then point your `frontend` project at your _local_ Identity, not the `CODE` Identity, which means modifying your `~/.gu/frontend.properties` as follows:

```
id.apiRoot=https://idapi.thegulocal.com
id.apiClientToken=frontend-dev-client-token
```

Once complete you will need to run the following instructions on the `frontend` project

```
nginx/setup.sh
./sbt
project identity
idrun
```

## Additional Documentation

Further documentation notes and useful items can be found in [docs](/docs).

- [Troubleshooting](docs/Troubleshooting.md) for information on common problems and how to fix them.
- [Building AMIs](docs/building-amis.md) for how to update our AMIs
