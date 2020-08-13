/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/facebook',
    'src/modules/analytics/uet',
    'src/modules/analytics/campaignCode',
    'src/modules/analytics/cmp',
    'src/modules/analytics/remarketing',
], function (
    cookie,
    ga,
    facebook,
    uet,
    campaignCode,
    cmp,
    remarketing
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

    function reportTagLoadFail(tracker, allPurposesAgreed) {
        console.log(`Either there's insufficient consent for ${tracker.vendorName}, or the user has ` +
            `turned that vendor off in the CMP (${tracker.cmpVendorId}). ` +
            `The user has ${allPurposesAgreed ? '' : 'not'} agreed to all purposes.`);
    }

    function init() {
        if (analyticsEnabled && !guardian.isDev) {
            const trackers = [ga, facebook, uet, remarketing];
            const vendorIds = trackers.map(tracker => tracker.cmpVendorId);

            Promise.allSettled([
                cmp.checkCCPA(),
                cmp.getConsentForVendors(vendorIds),
                cmp.checkAllTCFv2PurposesAreOptedIn(),
            ]).then(results => {
                const [ccpaConsent, vendorConsents, allPurposesAgreed] = results.map(promise => promise.value);

                if (ccpaConsent) {
                    trackers.forEach(tracker => tracker.init());
                    campaignCode.init();
                } else {
                    trackers.forEach(tracker => {
                        vendorConsents[tracker.cmpVendorId] ?
                            tracker.init() : reportTagLoadFail(tracker, allPurposesAgreed)
                    })
                    allPurposesAgreed && campaignCode.init()
                }
            });
        }
    }

    return {
        init: init,
        enabled: analyticsEnabled
    };
});
