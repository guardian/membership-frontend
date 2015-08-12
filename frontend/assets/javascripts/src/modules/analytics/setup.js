/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/ophan',
    'src/modules/analytics/omniture',
    'src/modules/analytics/krux',
    'src/modules/analytics/appnexus',
    'src/modules/analytics/twitter'
], function (
    cookie,
    ga,
    ophan,
    omniture,
    krux,
    appnexus,
    twitter
) {
    'use strict';

    var analyticsEnabled = (
        guardian.analyticsEnabled &&
        !navigator.doNotTrack &&
        !cookie.getCookie('ANALYTICS_OFF_KEY')
    );

    function setupAnalytics() {
        ophan.init();
        omniture.init();
        ga.init();
    }

    function setupThirdParties() {
        krux.load();
        appnexus.load();
        twitter.load();
    }

    function init() {

        if (analyticsEnabled) {
            setupAnalytics();
        }

        if(analyticsEnabled && !guardian.isDev) {
            setupThirdParties();
        }
    }

    return {
        init: init
    };
});
