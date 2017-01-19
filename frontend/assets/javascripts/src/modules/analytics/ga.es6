import * as user  from 'src/utils/user'
import * as cookie from 'src/utils/cookie'

const tracker = 'membershipPropertyTracker';
const dimensions = {
    signedIn: 'dimension1', // User
    signedOut: 'dimension2', // User
    ophanPageViewId: 'dimension3', // Hit
    ophanBrowserId: 'dimension4', // User
    platform: 'dimension5', // Hit
    identityId: 'dimension6', // User
    isLoggedOn: 'dimension7', // Hit
    stripeId: 'dimension8', // Session
    zouraId: 'dimension9', // Session
    membershipNumber: 'dimension10', // User
    productPurchased: 'dimension11', // Session
    intcmp: 'dimension12', // Session
    customerAgent: 'dimension13', // Session
    CamCodeBusinessUnit: 'dimension14', // Session
    CamCodeTeam: 'dimension15', // Session
    experience: 'dimension16',// Session
    paymentMethod: 'dimension17',// User

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
    if (guardian.abPriceCTA) {
        wrappedGa('set', dimensions.experience, guardian.abPriceCTA);
    }

    if (isLoggedIn) {
        wrappedGa('set', dimensions.identityId, u.id);
    }
    if (guardian.ophan) {
        wrappedGa('set', dimensions.ophanPageViewId, guardian.ophan.pageViewId);
    }
    if("productData" in guardian) {

        wrappedGa('set',dimensions.membershipNumber,guardian.productData.regNumber);
        wrappedGa('set',dimensions.productPurchased,guardian.productData.tier);
        wrappedGa('set',dimensions.paymentMethod,guardian.productData.paymentMethod);
        // Send analytics after acquisition (on thank you page).
        acquisitionEvent();

    }
    wrappedGa('set', dimensions.ophanBrowserId, cookie.getCookie('bwid'));

    let intcmp = new RegExp('INTCMP=([^&]*)').exec(location.search);
    if (intcmp && intcmp[1]){
        wrappedGa('set',dimensions.intcmp,intcmp[1]);
    }
    let cmpBunit = new RegExp('CMP_BUNIT=([^&]*)').exec(location.search);
    if (cmpBunit && cmpBunit[1]){
        ga('set',dimensions.CamCodeBusinessUnit,cmpBunit[1]);
    }
    let cmpTu = new RegExp('CMP_TU=([^&]*)').exec(location.search);
    if (cmpTu && cmpTu[1]){
        ga('set',dimensions.CamCodeTeam,cmpTu[1]);
    }

    wrappedGa('send', 'pageview');

}
