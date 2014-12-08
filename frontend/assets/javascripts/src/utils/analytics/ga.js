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

        trackOutboundLinks();
    }

    // wrapper for tracking events via google analytics
    // (because analytics can be removed for test user mode)
    var trackEvent = function (category, action, label) {
        if (window.ga) {
            ga('send', 'event', category, action, label);
        }
    };

    var trackOutboundLinks = function() {
        var TRACKING_NAME = 'data-link-name',
            elems = document.querySelectorAll('[' + TRACKING_NAME + ']');
        if (elems.length) {
            [].forEach.call(elems, function( el ) {
                el.addEventListener('click', function() {
                    var targetElement = event.target || event.srcElement;
                    trackEvent('Click element', targetElement.getAttribute(TRACKING_NAME));
                });
            });
        }
    };

    return {
        init: init,
        trackEvent: trackEvent
    };
});
