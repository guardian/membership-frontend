/*global ga */
define([], function () {

    function init() {
        // Google analytics snippet
        /* jshint ignore:start */
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
        (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

        ga('create', guardian.googleAnalytics.trackingId, {
            'allowLinker': true,
            'cookieDomain': guardian.googleAnalytics.cookieDomain
        });

        ga('require', 'linker');
        ga('linker:autoLink', ['eventbrite.co.uk'] );

        ga('send', 'pageview');
        /* jshint ignore:end */

        if (window.ga) {
            trackOutboundLinks();
        }
    }

    // wrapper for tracking events via google analytics
    // (because analytics can be removed for test user mode)
    function trackEvent(category, action, label, data) {
        data = data || {};
        if (window.ga) {
            ga('send', 'event', category, action, label, data);
        }
    }

    function trackOutboundLinks() {
        var TRACKING_NAME = 'data-link-name',
            elems = document.querySelectorAll('[' + TRACKING_NAME + ']');
        if (elems.length) {
            [].forEach.call(elems, function( el ) {
                el.addEventListener('click', function(event) {
                    var targetElement = event.target || event.srcElement,
                        action = targetElement.getAttribute(TRACKING_NAME),
                        url = targetElement.getAttribute('href');

                    trackEvent('outbound', action, url, {
                        'hitCallback': function () {
                            if (url) {
                                document.location = url;
                            }
                        }
                    });
                });
            });
        }
    }

    return {
        init: init,
        trackEvent: trackEvent
    };
});
