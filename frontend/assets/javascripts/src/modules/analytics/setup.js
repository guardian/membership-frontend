/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/omniture'
], function (cookie, googleAnalytics, omnitureAnalytics) {

    var ANALYTICS_OFF_KEY = 'ANALYTICS_OFF_KEY';

    function init() {
        if (cookie.getCookie(ANALYTICS_OFF_KEY)) {
            guardian.analyticsEnabled = false;
        }

        if (guardian.analyticsEnabled) {
            require('ophan/membership', function () {});
            omnitureAnalytics.init();
            googleAnalytics.init();
        }
    }

    return {
        init: init
    };
});
