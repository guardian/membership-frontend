/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/ophan',
    'src/modules/analytics/omniture',
    'src/modules/analytics/krux',
    'src/modules/analytics/crazyegg'
], function (
    cookie,
    googleAnalytics,
    ophanAnalytics,
    omnitureAnalytics,
    krux,
    crazyegg
) {

    var ANALYTICS_OFF_KEY = 'ANALYTICS_OFF_KEY';

    function init() {
        if (cookie.getCookie(ANALYTICS_OFF_KEY)) {
            guardian.analyticsEnabled = false;
        }

        guardian.analyticsEnabled = true;
        if (guardian.analyticsEnabled) {
            ophanAnalytics.init();
            omnitureAnalytics.init();
            googleAnalytics.init();

            krux.load();

            if(!guardian.isDev) {
                crazyegg.load();
            }

        }
    }

    return {
        init: init
    };
});
