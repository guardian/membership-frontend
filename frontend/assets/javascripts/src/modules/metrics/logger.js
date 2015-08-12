/* global ga */
define(['lodash/object/extend'], function (extend) {
    'use strict';

    return function(options) {
        var metricData;
        if (window.ga) {
            metricData = extend({
                'hitType': 'event'
            }, options);
            ga('send', metricData);
        }
    };
});
