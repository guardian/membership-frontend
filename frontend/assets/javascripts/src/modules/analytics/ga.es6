import * as user  from 'src/utils/user'
import * as cookie from 'src/utils/cookie'
import { loadScript } from 'src/utils/loadScript';

const tracker = 'membershipPropertyTracker';
const dimensions = {
    signedIn: 'dimension1', // User
    signedOut: 'dimension2', // User
    // Removed -- ophanPageViewId: 'dimension3', // Hit
    // Removed -- ophanBrowserId: 'dimension4', // User
    platform: 'dimension5', // Hit
    // Removed -- identityId: 'dimension6', // User
    isLoggedOn: 'dimension7', // Hit
    // Never sent -- stripeId: 'dimension8', // Session
    zouraId: 'dimension9', // Session
    // Removed -- membershipNumber: 'dimension10', // User
    productPurchased: 'dimension11', // Session
    intcmp: 'dimension12', // Session
    customerAgent: 'dimension13', // Session
    CamCodeBusinessUnit: 'dimension14', // Session
    CamCodeTeam: 'dimension15', // Session
    experience: 'dimension16',// Session
    paymentMethod: 'dimension17',// Hit

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
    /**
     * Instruction for Google Analytics
     * to leverage the TCFv2 framework
    */
    window.gtag_enable_tcf_support = true;

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

    addGtagForGA4();
}

function addGtagForGA4 () {
    window.dataLayer = window.dataLayer || [];

    function gtag() {
        window.dataLayer.push(arguments);
    }

    loadScript('https://www.googletagmanager.com/gtag/js?id=G-5SVJVDLPW0').then(() => {
        gtag('js', new Date());
        gtag('config', 'G-5SVJVDLPW0');
    } )
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

    if (guardian.abTests) {
        wrappedGa('set', dimensions.experience, Object.keys(guardian.abTests).map(function(k){return k+'='+guardian.abTests[k]}).join(','));
    }

    // The hash on the url is set in identity-federation-api to indicate user has come via facebook login, this identifies that and stops the referrer being counted as www.facebook.com
    if(document.location.hash === '#fbLogin') {
        wrappedGa('set', 'referrer', null);
        document.location.hash = '';
    }

    if('productData' in guardian) {

        wrappedGa('set',dimensions.productPurchased,guardian.productData.tier);
        wrappedGa('set',dimensions.paymentMethod,guardian.productData.paymentMethod);
        // Send analytics after acquisition (on thank you page).
        acquisitionEvent();

    }

    let intcmp = new RegExp('INTCMP=([^&]*)').exec(location.search);
    if (intcmp && intcmp[1]){
        wrappedGa('set',dimensions.intcmp,intcmp[1]);
    }
    let cmpBunit = new RegExp('CMP_BUNIT=([^&]*)').exec(location.search);
    if (cmpBunit && cmpBunit[1]){
        window.ga('set',dimensions.CamCodeBusinessUnit,cmpBunit[1]);
    }
    let cmpTu = new RegExp('CMP_TU=([^&]*)').exec(location.search);
    if (cmpTu && cmpTu[1]){
        window.ga('set',dimensions.CamCodeTeam,cmpTu[1]);
    }

    wrappedGa('send', 'pageview');

}

export const vendorName = 'Google Analytics'
export const cmpVendorId = '5e542b3a4cd8884eb41b5a72'
