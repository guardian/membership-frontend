/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/ophan',
    'src/modules/analytics/omniture',
    'src/modules/analytics/krux',
    'src/modules/analytics/facebook',
    'src/modules/analytics/uet'
], function (
    cookie,
    ga,
    ophan,
    omniture,
    krux,
    facebook,
    uet
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
        uet.init();
    }

    function setupThirdParties() {
        krux.init();
        facebook.init();
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
