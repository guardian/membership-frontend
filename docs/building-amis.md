# Building AMIs

We use [packer](http://www.packer.io) to create new AMIs, you can download it here: http://www.packer.io/downloads.html.
If you are on OS X and using Homebrew, you can install Packer by adding the `binary` tap:
```
brew tap homebrew/binary
brew install packer
```


To create an AMI, you must set `AWS_ACCESS_KEY` and `AWS_SECRET_KEY` as described above.

To add your requirements to the new AMI, you should update `provisioning.json`. This will probably involve editing the `provisioners` section, but more information can be found in the [packer docs](http://www.packer.io/docs).

To get the latest list of AMIs you can use the following AWS command:
```
aws ec2 describe-images  --owner 099720109477 --region eu-west-1 --filter "Name=architecture,Values=x86_64" --filter "Name=virtualization-type,Values=hvm" | jq '.Images[] | select(contains({RootDeviceType: "ebs"})) | .ImageId + " " + .Name' | grep hvm-ssd/ubuntu-trusty-14.04-amd64-server | sort -r --key 2 | head -6
```

(Note the example above uses ```ubuntu-trusty-14.04-amd64-server``` but feel free to use a more up to releases.)

Once you are ready, run the following:
```
packer build provisioning.json
```
This will take several minutes to build the new AMI. Once complete, you should see something like:
```
eu-west-1: ami-xxxxxxxx
```

Use the Deployment steps below to test your new AMI

1. Turn off continuous deployment in RiffRaff
1. Update the CloudFormation parameter `ImageId` <b>(make a note of the current value first)</b>
1. Increase the autoscale group size by 1
1. Test the new box
1. If it doesn't work, revert the value of `ImageId`
1. Run a full deploy in RiffRaff
1. Decrease autoscale group size by 1
1. Re-enable continous deployment

