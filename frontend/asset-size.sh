#!/bin/bash

STYLESHEETS=("public/dist/stylesheets/**/*.css")

for stylesheet in ${STYLESHEETS[@]}; do
    name=`basename $stylesheet`
    size=`cat $stylesheet | wc -c`
    gzip_size=`gzip -c $stylesheet | wc -c`

    aws cloudwatch put-metric-data --namespace TestGlob --metric-name $name --dimensions "Compression=None" --value $size
    aws cloudwatch put-metric-data --namespace TestGlob --metric-name $name --dimensions "Compression=Gzip" --value $gzip_size
done

JAVASCRIPTS=("public/dist/javascripts/*/*.js")

for javascript in ${JAVASCRIPTS[@]}; do
    name=`basename $javascript`
    size=`cat $javascript | wc -c`
    gzip_size=`gzip -c $javascript | wc -c`

    aws cloudwatch put-metric-data --namespace TestGlob --metric-name $name --dimensions "Compression=None" --value $size
    aws cloudwatch put-metric-data --namespace TestGlob --metric-name $name --dimensions "Compression=Gzip" --value $gzip_size
done
