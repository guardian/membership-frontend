Fastly logs
===========

Fastly periodically dumps logs as new files into S3 (using an IAM user called 'fastly-log-writer' in our AWS Account with a
minimal IAM policy) and it's possible to choose the frequency of updates - sometimes it's handy to get really recent data
(which means lots of small files), and sometimes you just want historical data and you'd much prefer to download bigger files.
So there are 3 folders, each with different logging periods configured:

* fastly/recent - updated every 2 minutes, deleted after 1 day
* fastly/hourly - updated every hour, deleted after 1 week
* fastly/daily - updated every day, deleted after 2 months

So if you want to grab the hourly logs, you can go:

```
$ cd `mktemp -d` ; aws s3 --profile membership cp --recursive s3://membership-logs/fastly/hourly/ .
```

You can then use `zgrep` to search for relevant lines.

Appserver CloudWatch Logs
=========================

Use the excellent [`awslogs`](https://github.com/jorgebastida/awslogs) utility to avoid the fairly-awful AWS console UI.

To tail logs in 'real-time':

```
$ awslogs get FrontendLogs-PROD ALL --watch --profile membership --aws-region eu-west-1 
```

To download a specific time period and append to a file:

```
$ awslogs get SubscriptionFrontend-PROD ALL --start='22/4/2017' --end='23/4/2017' --profile membership --aws-region eu-west-1 >> subscriptions-frontend-22042017_23042017.log
```
