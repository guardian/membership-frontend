# Membership Frontend

[https://membership.theguardian.com/](https://membership.theguardian.com/). Bringing the Guardian to life through live events and meet-ups.

## Table of Contents

1. [Getting Started](#getting-started)
1. [Setup](#setup)
1. [Run](#run)
1. [Tests](#tests)
1. [Test Users](#test-users)
1. [Security](#security)
1. [Additional Documentation](#additional)

## Getting Started

To get started working on Membership you will need to complete the following steps:

1. Work through the [Setup](#Setup) instructions for this project
2. Work through the setup instructions for the [theguardian.com](https://github.com/guardian/frontend) and its subproject [Identity](https://github.com/guardian/frontend/tree/master/identity). This provides a frontend for sign-up and registration.
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

To run standalone you can use the default nginx installation as follows:

Install nginx:

- Linux: `sudo apt-get install nginx`
- Mac OSX: `brew install nginx`

Make sure you have a `sites-enabled/` folder under your nginx home. This varies depending on your setup but should be under:

- Linux: `/etc/nginx/sites-enabled`
- Mac OSX: `/usr/local/etc/nginx/sites-enabled`

Make sure your `nginx.conf` (found in your nginx home) contains the following line in the `http{...}` block:

```
include sites-enabled/*
```

If you want an automated setup run:

```
./nginx/setup.sh
```

Or follow the steps in that file.

### Update your hosts file

Add the following to your hosts file in `/etc/hosts`:

```
127.0.0.1   mem.thegulocal.com
127.0.0.1   profile.thegulocal.com
```

### Setup AWS credentials

Setup AWS credentials. Ask your teammate to create an account for you and securely send you the access key. For security, you must enable [MFA](http://aws.amazon.com/iam/details/mfa/).

In `~/.aws/credentials` add the following:

```
[membership]
aws_access_key_id = [YOUR_AWS_ACCESS_KEY]
aws_secret_access_key = [YOUR_AWS_SECRET_ACCESS_KEY]
region = eu-west-1

```

### Download private keys

Download our private keys from the `membership-private` S3 bucket. If you have the AWS CLI set up you can run:

```
aws s3 cp s3://membership-private/DEV/membership-keys.conf /etc/gu --profile membership
```

### Ubuntu setup

In an ideal world, your Ubuntu package install would be:

```
sudo apt-get install nginx openjdk-7-jdk nodejs npm
```

#### [Node](http://nodejs.org/) & [NPM](https://github.com/npm/npm/releases)

See Joyent's instructions on [installing Node & NPM on Ubuntu](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os).
If the available [nodejs](http://packages.ubuntu.com/trusty/nodejs) or [NPM](http://packages.ubuntu.com/trusty/npm)
package for your version of Ubuntu is old, you'll [probably](http://askubuntu.com/questions/49390/how-do-i-install-the-latest-version-of-node-js)
want to install [chris-lea's PPA](https://launchpad.net/~chris-lea/+archive/node.js),
which includes both Node.js and NPM.

## Run

The app normally runs on port `9100`. You can run the following commands to start the app (separate console windows)

```
 nginx/setup.sh
./start-frontend.sh
```



## Tests

### Running Scala unit tests

To run Scala unit tests run `sbt test` from the root of the project.

### Running JavaScript unit tests

```
cd frontend/
npm test
```

### Running functional tests

See [README.md](functional-tests/README.md) for details on how to run Membership functional tests.

## Test Users

[See here](https://sites.google.com/a/guardian.co.uk/guardan-identity/identity/test-users) for details of how we do test users.
Note that we read the shared secret for these from the `identity.test.users.secret` property in `membership-keys.conf`.

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

**Identity repo**: [https://github.com/guardian/identity](https://github.com/guardian/identity)

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

