/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/omniture',
    'src/modules/analytics/userzoom'
], function (cookie, googleAnalytics, omnitureAnalytics, userzoom) {

    var ANALYTICS_OFF_KEY = 'ANALYTICS_OFF_KEY';

    function init() {
        if (cookie.getCookie(ANALYTICS_OFF_KEY)) {
            guardian.analyticsEnabled = false;
        }

        if (guardian.analyticsEnabled) {
            require('ophan/membership', function () {});
            omnitureAnalytics.init();
            googleAnalytics.init();

            if(!guardian.isDev) {
                userzoom.load();
            }

        }
    }

    return {
        init: init
    };
});
