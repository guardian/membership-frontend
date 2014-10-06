```
                            __                            __
 /'\_/`\                   /\ \                          /\ \      __
/\      \     __    ___ ___\ \ \____     __   _ __   ____\ \ \___ /\_\  _____
\ \ \__\ \  /'__`\/' __` __`\ \ '__`\  /'__`\/\`'__\/',__\\ \  _ `\/\ \/\ '__`\
 \ \ \_/\ \/\  __//\ \/\ \/\ \ \ \L\ \/\  __/\ \ \//\__, `\\ \ \ \ \ \ \ \ \L\ \
  \ \_\\ \_\ \____\ \_\ \_\ \_\ \_,__/\ \____\\ \_\\/\____/ \ \_\ \_\ \_\ \ ,__/
   \/_/ \/_/\/____/\/_/\/_/\/_/\/___/  \/____/ \/_/ \/___/   \/_/\/_/\/_/\ \ \/
                                                                          \ \_\
                                                                           \/_/
```

# Membership App

## Ubuntu

In an ideal world, your Ubuntu package install would be:

```
$ sudo apt-get install nginx openjdk-7-jdk ruby ruby-dev nodejs npm
```

### [Node](http://nodejs.org/) & [NPM](https://github.com/npm/npm/releases)

See Joyent's instructions on [installing Node & NPM on Ubuntu](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os).
If the available [nodejs](http://packages.ubuntu.com/trusty/nodejs) or [NPM](http://packages.ubuntu.com/trusty/npm)
package for your version of Ubuntu is old, you'll [probably](http://askubuntu.com/questions/49390/how-do-i-install-the-latest-version-of-node-js)
want to install [chris-lea's PPA](https://launchpad.net/~chris-lea/+archive/node.js),
which includes both Node.js and NPM.

## General Setup


1. Go to project root
1. If you don't have `bower`, `grunt`, or `sass`, run `./setup-tools.sh` to install them globally on your system.
1. Run `./setup.sh` to install project-specific client-side dependencies.
1. Add the following to your `/etc/hosts`

   ```
   127.0.0.1   mem.thegulocal.com
   ```

1. ./nginx/setup.sh
1. Download our private keys from the `membership-private` S3 bucket. You will need an AWS account so ask another dev.

    If you have the AWS CLI set up you can run
    ```
    aws s3 cp s3://membership-private/DEV/membership-keys.conf /etc/gu
    ```
1. In ~/.bash_profile add
    ```
    export AWS_ACCESS_KEY=<access-key-id>

    export AWS_SECRET_KEY=<secret-key>
    ```

## Run
The app normally runs on ports 9100 respectively.
You can run the following commands to start them (separate console windows)

```
 nginx/setup.sh
./start-frontend.sh
```

## Run membership locally with Identity on NGW
To be able to go through the registration flow locally you will need to have the Identity API and Identity project in NGW running.

Identity repo: https://github.com/guardian/identity

Run through the set up instructions. Once complete you will just need to run
```
 nginx/setup.sh
 ./start-api.sh
```

NGW Frontend repo: https://github.com/guardian/frontend

Run through the set up instructions. Once complete you will just need to run
```
nginx/setup.sh
./sbt
project identity
idrun
```

## To run frontend tests

+ $ karma start

# Grunt Tasks

## Watch and compile front-end files
+ $ grunt watch

## Compile front-end files
+ $ grunt compile

# Updating AMIs
We use [packer](http://www.packer.io) to create new AMIs, you can download it here: http://www.packer.io/downloads.html. To create an AMI, you must set `AWS_ACCESS_KEY` and `AWS_SECRET_KEY` as described above.

## Building

To add your requirements to the new AMI, you should update `provisioning.json`. This will probably involve editing the `provisioners` section, but more information can be found in the [packer docs](http://www.packer.io/docs). Once you are ready, run the following:
```
packer build provisioning.json
```
This will take several minutes to build the new AMI. Once complete, you should see something like:
```
eu-west-1: ami-xxxxxxxx
```

## Deploying

1. Turn off continuous deployment in RiffRaff
1. Update the CloudFormation parameter `ImageId` <b>(make a note of the current value first)</b>
1. Increase the autoscale group size by 1
1. Test the new box
1. If it doesn't work, revert the value of `ImageId`
1. Run a full deploy in RiffRaff
1. Decrease autoscale group size by 1
1. Re-enable continous deployment
