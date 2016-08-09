import * as user  from 'src/utils/user'
import * as cookie from 'src/utils/cookie'

const dimensions = {
    signedIn: 'dimension1',
    signedOut: 'dimension2',
    ophanPageViewId: 'dimension3',
    ophanBrowserId: 'dimension4',
    platform: 'dimension5',
    identityId: 'dimension6',
    isLoggedOn: 'dimension7'
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
    const tracker = 'membershipPropertyTracker';
    window.ga('create', guardian.googleAnalytics.trackingId, {
        'allowLinker': true,
        'name': tracker,
        'cookieDomain': guardian.googleAnalytics.cookieDomain
    });
    return (a,b,c) => window.ga(tracker+ '.' + a,b,c);
}

export function init() {
    let guardian = window.guardian;
    let ga = create();

    ga('require', 'linker');

    /**
     * Enable enhanced link attribution
     * https://support.google.com/analytics/answer/2558867?hl=en-GB
     */
    ga('require', 'linkid', 'linkid.js');


    //Set the custom dimensions.
    let u = user.getUserFromCookie();
    let isLoggedIn = !!u;
    let signedOut = !!cookie.getCookie('GU_SO') && !isLoggedIn;
    ga('set', dimensions.signedIn, isLoggedIn.toString());
    ga('set', dimensions.isLoggedOn, isLoggedIn.toString());
    ga('set', dimensions.signedOut, signedOut.toString());
    ga('set', dimensions.platform, 'membership');
    if (isLoggedIn) {
        ga('set', dimensions.identityId, u.id);
    }
    if (guardian.ophan) {
        ga('set', dimensions.ophanPageViewId, guardian.ophan.pageViewId);
    }
    ga('set', dimensions.ophanBrowserId, cookie.getCookie('bwid'));
    //Send the pageview.
    ga('send', 'pageview');


}

export function pageView(name) {
    ga('set', 'page', '/#' + name);
    ga('send', 'pageview');
}
