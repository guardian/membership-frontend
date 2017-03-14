define(['lodash/extend','src/modules/analytics/ga'], function (extend, ga) {
    'use strict';

    return function(options) {
        var metricData;
        if (window.ga) {
            metricData = extend({
                'hitType': 'event'
            }, options);
            ga.wrappedGa('send', metricData);
        }
    };
});
