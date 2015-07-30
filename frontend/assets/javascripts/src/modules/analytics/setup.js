/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/ophan',
    'src/modules/analytics/omniture',
    'src/modules/analytics/krux',
    'src/modules/analytics/appnexus',
    'src/modules/analytics/twitter',
    'src/modules/analytics/crazyegg',
    'src/modules/analytics/optimizely'
], function (
    cookie,
    ga,
    ophan,
    omniture,
    krux,
    appnexus,
    twitter,
    crazyegg,
    optimizely
) {

    function setupAnalytics() {
        ophan.init();
        omniture.init();
        ga.init();
    }

    function setupThirdParties() {
        krux.load();
        appnexus.load();
        twitter.load();
        crazyegg.load();
        optimizely.init();
    }

    function init() {
        if (cookie.getCookie('ANALYTICS_OFF_KEY')) {
            guardian.analyticsEnabled = false;
        }

        if (guardian.analyticsEnabled) {
            setupAnalytics();
        }

        if(guardian.analyticsEnabled && !guardian.isDev) {
            setupThirdParties();
        }
    }

    return {
        init: init
    };
});
