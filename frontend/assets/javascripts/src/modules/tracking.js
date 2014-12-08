define(['src/utils/analytics/ga'], function (googleAnalytics) {

    var TRACKING_CLICK_SELECTOR = '.js-track-click',
        TRACKING_NAME = 'data-tracking-name';

    function trackClick() {
        var elems = document.querySelectorAll(TRACKING_CLICK_SELECTOR);
        if (elems.length) {
            [].forEach.call(elems, function( el ) {
                el.addEventListener('click', function() {
                    var targetElement = event.target || event.srcElement;
                    var action = targetElement.getAttribute(TRACKING_NAME);
                    googleAnalytics.trackEvent('Click element', action);
                });
            });
        }
    }

    function init() {
        trackClick();
    }

    return {
        init: init
    };

});
