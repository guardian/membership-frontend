import * as user  from 'src/utils/user'
import * as cookie from 'src/utils/cookie'

const tracker = 'membershipPropertyTracker';
const dimensions = {
    signedIn: 'dimension1',
    signedOut: 'dimension2',
    ophanPageViewId: 'dimension3',
    ophanBrowserId: 'dimension4',
    platform: 'dimension5',
    identityId: 'dimension6',
    isLoggedOn: 'dimension7',
    stripeId: 'dimension8',
    zouraId: 'dimension9',
    membershipNumber: 'dimension10',
    productPurchased: 'dimension11',
    intcmp: 'dimension12'
};
const metrics = {
    join: {
        Friend: 'metric3',
        Supporter: 'metric4',
        Partner: 'metric5',
        Patron: 'metric6'
    },
    upgrade: {
        Supporter: 'metric7',
        Partner: 'metric8',
        Patron: 'metric9'
    }
};
let _EVENT_QUEUE = [];

function create(){
    /*eslint-disable */
    (function (i, s, o, g, r, a, m) {
        i['GoogleAnalyticsObject'] = r;
        i[r] = i[r] || function () {
                (i[r].q = i[r].q || []).push(arguments)
            }, i[r].l = 1 * new Date();
        a = s.createElement(o),
            m = s.getElementsByTagName(o)[0];
        a.async = 1;
        a.src = g;
        m.parentNode.insertBefore(a, m)
    })(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');
    /*eslint-enable */
    window.ga('create', guardian.googleAnalytics.trackingId, {
        'allowLinker': true,
        'name': tracker,
        'cookieDomain': guardian.googleAnalytics.cookieDomain
    });
}

// Queues up tracked events on the page, attempts to send to Google.
function acquisitionEvent () {

    let event = {
        eventCategory: 'Membership Acquisition',
        eventAction: guardian.productData.upgrade ? 'Upgrade' : 'Join'
    };

    if (guardian.productData.upgrade) {
        event[metrics.upgrade[guardian.productData.tier]] = 1;
    } else {
        event[metrics.join[guardian.productData.tier]] = 1;
    }

    wrappedGa('send', 'event', event);

}

export function wrappedGa(a,b,c){
    return window.ga(tracker+ '.' + a,b,c);

}

export function init() {
    let guardian = window.guardian;
    create();

    wrappedGa('require', 'linker');

    /**
     * Enable enhanced link attribution
     * https://support.google.com/analytics/answer/2558867?hl=en-GB
     */
    wrappedGa('require', 'linkid', 'linkid.js');


    //Set the custom dimensions.
    let u = user.getUserFromCookie();
    let isLoggedIn = !!u;
    let signedOut = !!cookie.getCookie('GU_SO') && !isLoggedIn;
    wrappedGa('set', dimensions.signedIn, isLoggedIn.toString());
    wrappedGa('set', dimensions.isLoggedOn, isLoggedIn.toString());
    wrappedGa('set', dimensions.signedOut, signedOut.toString());
    wrappedGa('set', dimensions.platform, 'membership');
    if (isLoggedIn) {
        wrappedGa('set', dimensions.identityId, u.id);
    }
    if (guardian.ophan) {
        wrappedGa('set', dimensions.ophanPageViewId, guardian.ophan.pageViewId);
    }
    if("productData" in guardian) {
        wrappedGa('set',dimensions.membershipNumber,guardian.productData.regNumber);
        wrappedGa('set',dimensions.productPurchased,guardian.productData.tier);
    }
    wrappedGa('set', dimensions.ophanBrowserId, cookie.getCookie('bwid'));

    let intcmp = new RegExp('INTCMP=([^&]*)').exec(location.search);
    if (intcmp && intcmp[1]){
        wrappedGa('set',dimensions.intcmp,intcmp[1]);
    }

    //Send the pageview.
    wrappedGa('send', 'pageview');
    // Send analytics after acquisition (on thank you page).
    if ('productData' in guardian) acquisitionEvent();

}
