/*global guardian:true */
define([
    'src/utils/analytics/ga',
    'src/utils/analytics/omniture',
    'src/utils/cookie'
], function (googleAnalytics, omnitureAnalytics, cookie) {

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
