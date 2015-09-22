/* global ga: true */
define(['src/utils/user'], function(user) {
    'use strict';

    var dimensions = {
        signedIn: 'dimension1',
        member: 'dimension2'
    };

    function init() {
        /*eslint-disable */
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
        (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        /*eslint-enable */

        var isLoggedIn = user.isLoggedIn();

        ga('create', guardian.googleAnalytics.trackingId, {
            'allowLinker': true,
            'cookieDomain': guardian.googleAnalytics.cookieDomain
        });

        ga('require', 'linker');
        ga('linker:autoLink', ['eventbrite.co.uk'] );

        /**
         * Enable enhanced link attribution
         * https://support.google.com/analytics/answer/2558867?hl=en-GB
         */
        ga('require', 'linkid', 'linkid.js');


        ga('set', dimensions.signedIn, isLoggedIn.toString());

        if(isLoggedIn) {
            user.getMemberDetail(function(memberDetail, hasTier) {
                ga('set', dimensions.member, hasTier.toString());
                ga('send', 'pageview');
            });
        } else {
            ga('set', dimensions.member, 'false');
            ga('send', 'pageview');
        }
    }

    return {
        init: init
    };

});
