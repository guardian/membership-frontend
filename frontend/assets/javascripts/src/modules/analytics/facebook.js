/* global fbq */
/* eslint-disable no-unused-vars, no-underscore-dangle */
define(['lodash/collection/forEach'], function(forEach) {
    'use strict';

    var FACEBOOK_PROPERTY_ID = '869473446445427';

    var URL_MAPPINGS = {
        '/join/supporter/thankyou': recordConversion
    };

    var EVENT_MAPPINGS = {
        '/': 'Lead',
        '/choose-tier': 'Lead',

        '/join/friend/enter-details': 'InitiateCheckout',
        '/join/supporter/enter-details': 'InitiateCheckout',
        '/join/partner/enter-details': 'InitiateCheckout',
        '/join/patron/enter-details': 'InitiateCheckout',

        '/join/friend/thankyou': 'CompleteRegistration',
        '/join/supporter/thankyou': 'CompleteRegistration',
        '/join/partner/thankyou': 'CompleteRegistration',
        '/join/patron/thankyou': 'CompleteRegistration'
    };

    function callByPathname(mappings, callback) {
        var pathname = window.location.pathname;
        forEach(mappings, function(val, url) {
            if(url === pathname && callback) {
                callback.call(null, val, url);
            }
        });
    }

    function loadEventTracking() {
        /*eslint-disable */
        !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
        n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
        n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
        t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
        document,'script','//connect.facebook.net/en_US/fbevents.js');
        /*eslint-enable */
    }

    function loadConversionTracking() {
        /*eslint-disable */
        var _fbq = window._fbq || (window._fbq = []);
        if (_fbq.loaded) { return; }
        var fbds = document.createElement('script');
        fbds.async = true; fbds.src = '//connect.facebook.net/en_US/fbds.js';
        var s = document.getElementsByTagName('script')[0];
        s.parentNode.insertBefore(fbds, s);
        _fbq.loaded = true;
        /*eslint-enable */
    }

    function recordConversion() {
        window._fbq = window._fbq || [];
        window._fbq.push(['track', '6030012243589', {'value': '0.00', 'currency': 'GBP'}]);
    }

    function init() {

        loadEventTracking();
        loadConversionTracking();

        fbq('init', FACEBOOK_PROPERTY_ID);
        fbq('track', 'PageView');
        fbq('track', 'ViewContent');

        callByPathname(URL_MAPPINGS, function(tracking) {
            tracking.call(null);
        });

        callByPathname(EVENT_MAPPINGS, function(evt) {
            fbq('track', evt);
        });
    }

    return {
        init: init
    };

});
