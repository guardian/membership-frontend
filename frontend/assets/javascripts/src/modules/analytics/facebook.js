define(function() {
    'use strict';

    function usSupporterLandingPage() {
        /*eslint-disable */
        !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
        n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
        n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
        t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
        document,'script','//connect.facebook.net/en_US/fbevents.js');

        fbq('init', '869473446445427');
        fbq('track', 'PageView');
        /*eslint-enable */
    }

    function supporterThankyou() {
        /*eslint-disable */
        (function() {
            var _fbq = window._fbq || (window._fbq = []);
            if (!_fbq.loaded) {
                var fbds = document.createElement('script');
                fbds.async = true;
                fbds.src = '//connect.facebook.net/en_US/fbds.js';
                var s = document.getElementsByTagName('script')[0];
                s.parentNode.insertBefore(fbds, s);
                _fbq.loaded = true;
            }
        })();
        window._fbq = window._fbq || [];
        window._fbq.push(['track', '6030012243589', {'value':'0.00','currency':'GBP'}]);
        /*eslint-enable */
    }

    var urlMapping = {
        '/us/supporter': usSupporterLandingPage,
        '/join/supporter/thankyou': supporterThankyou
    };

    function init() {
        // Specific page tracking if we match a given path
        var tracking = urlMapping[window.location.pathname] || false;
        if(tracking) {
            tracking.call();
        }
    }

    return {
        init: init
    };

});
