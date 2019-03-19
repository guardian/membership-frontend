/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/krux',
    'src/modules/analytics/facebook',
    'src/modules/analytics/uet',
    'src/modules/analytics/campaignCode',
    'src/modules/analytics/thirdPartyTracking'
], function (
    cookie,
    ga,
    krux,
    facebook,
    uet,
    campaignCode,
    thirdPartyTracking
) {
    'use strict';

    /*
     Re: https://bugzilla.mozilla.org/show_bug.cgi?id=1023920#c2

     The landscape at the moment is:

     On navigator [Firefox, Chrome, Opera]
     On window [IE, Safari]
     */
    var isDNT = navigator.doNotTrack == '1' || window.doNotTrack == '1';

    var analyticsEnabled = (
        guardian.analyticsEnabled &&
        !isDNT &&
        !cookie.getCookie('ANALYTICS_OFF_KEY')
    );

    const thirdPartyTrackingEnabled = thirdPartyTracking.thirdPartyTrackingEnabled();

    function setupAnalytics() {
        ga.init();
    }

    function setupThirdParties() {
        krux.init();
        facebook.init();
        uet.init();
    }

    function init() {

        campaignCode.init();
        if (analyticsEnabled) {
            setupAnalytics();
        }

        if(analyticsEnabled && thirdPartyTrackingEnabled && !guardian.isDev) {
            setupThirdParties();
        }
    }

    return {
        init: init,
        enabled: analyticsEnabled
    };
});
