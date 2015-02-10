define([
    'src/modules/metrics/triggers',
    'src/modules/metrics/forms'
], function (metricsTriggers, metricsForms) {
    return {
        init: function() {
            metricsTriggers.init();
            metricsForms.init();
        }
    };
});
