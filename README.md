# Membership Frontend

Bringing the Guardian to life through live events and meet-ups

https://membership.theguardian.com/

## Table of Contents

1. [Getting Started](#getting-started)
1. [Setup](#setup)
1. [Run](#run)
1. [Client-side Development](#cs-development)
1. [Tests](#tests)
1. [Test Users](#test-users)
1. [Security](#security)
1. [Additional Documentation](#additional)

<a name="getting-started">
## Getting Started

To get started working on Membership you will need to complete the following steps:

1. Work through the **General Setup** instructions for this project
2. Work through the setup instructions for [Identity](https://github.com/guardian/identity) and [theguardian.com](https://github.com/guardian/identity)
3. Start up Membership by running the commands in the [Run](#run) section of this README

<a name="setup">
## Setup

1. Go to the project root
1. Run `./setup.sh` to install project-specific client-side dependencies
1. Change the ownership of the 'gu' directory under 'etc' to current user.
   `$ sudo -i chown -R {username} /etc/gu`
1. Add the following to your hosts file in `/etc/hosts`:

```
127.0.0.1   mem.thegulocal.com
```

1. Run `membership-frontend$ ./nginx/setup.sh`
1. Download our private keys from the `membership-private` S3 bucket. You will need an AWS account so ask another dev. If you have the AWS CLI set up you can run:

```
aws s3 cp s3://membership-private/DEV/membership-keys.conf /etc/gu --profile membership
```

1. Setup AWS credentials. Ask your teammate to create an account for you and securely send you the access key. For security, you must enable [MFA](http://aws.amazon.com/iam/details/mfa/).

In `~/.aws/credentials` add the following:

```
[membership]
aws_access_key_id = [YOUR_AWS_ACCESS_KEY]
aws_secret_access_key = [YOUR_AWS_SECRET_ACCESS_KEY]
region = eu-west-1

```

In `~/.aws/config` add the following:

```
[default]
output = json
region = eu-west-1
```

### Ubuntu

In an ideal world, your Ubuntu package install would be:

```
$ sudo apt-get install nginx openjdk-7-jdk ruby ruby-dev nodejs npm
```

#### [Node](http://nodejs.org/) & [NPM](https://github.com/npm/npm/releases)

See Joyent's instructions on [installing Node & NPM on Ubuntu](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os).
If the available [nodejs](http://packages.ubuntu.com/trusty/nodejs) or [NPM](http://packages.ubuntu.com/trusty/npm)
package for your version of Ubuntu is old, you'll [probably](http://askubuntu.com/questions/49390/how-do-i-install-the-latest-version-of-node-js)
want to install [chris-lea's PPA](https://launchpad.net/~chris-lea/+archive/node.js),
which includes both Node.js and NPM.

<a name="run">
## Run

The app normally runs on port `9100`. You can run the following commands to start the app (separate console windows)

```
 nginx/setup.sh
./start-frontend.sh
```

### Run membership locally with Identity on theguardian.com

To be able to go through the registration flow locally you will need to have the Identity API and Identity project in theguardian.com running.

**Identity repo**: [https://github.com/guardian/identity](https://github.com/guardian/identity)

Run through the set up instructions; once complete you will need to run:

```
 nginx/setup.sh
 ./start-api.sh
```

**theguardian.com frontend repo**: [https://github.com/guardian/frontend](https://github.com/guardian/frontend)

Run through the set up instructions - download `frontend` and make sure that your `frontend` project is set up to point at your _local_ Identity, not the `CODE` Identity, which means adding this to your `frontend.properties`:

 `$ cd ~/.gu`
 `.gu$ vi frontend.properties`

Add the below

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

<a name="tests">
## Tests

<a name="tests-scala">
### Running Scala unit tests

To run Scala unit tests run `sbt test` from the root of the project.

<a name="tests-js">
### Running JavaScript unit tests

**Note these commands should be run from inside the `frontend/` directory**

```
grunt karma --dev
```

#### Test coverage

Run with `--dev` flag to generate test coverage. Coverage report can be found in `frontend/test/js/coverage/`.

<a name="tests-functional">
### Running functional tests

See [README.md](functional-tests/README.md) for details on how to run Membership functional tests.

<a name="cs-development">
## Client-side Development

Client-side build are handled using Grunt. **Note these commands should be run from inside the `frontend/` directory**

#### Watch and compile front-end files

```
grunt watch --dev
```

#### Compile front-end files

```
grunt compile --dev
```

### Client-side Principles

See [client-side-principles.md](docs/client-side-principles.md) for high-level client-side principles for Membership.

<a name="pattern-library">
### Pattern Library

A library of common patterns used accross the membership site is available at [membership.theguardian.com/patterns](https://membership.theguardian.com/patterns)

When building new components break them down into fragments and include them in the pattern library.

<a name="test-users">
## Test Users

See https://sites.google.com/a/guardian.co.uk/guardan-identity/identity/test-users for details of how we do test users.
Note that we read the shared secret for these from the `identity.test.users.secret` property in `membership-keys.conf`.

<a name="security">
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

<a name="additional">
## Additional Documentation

Further documentation notes and useful items can be found in [docs](/docs).

- [Troubleshooting](docs/Troubleshooting.md) for information on common problems and how to fix them.
- [Building AMIs](docs/building-amis.md) for how to update our AMIs

