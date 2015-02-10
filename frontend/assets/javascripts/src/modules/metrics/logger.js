/* global ga */
define(['lodash/object/extend'], function (extend) {
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
