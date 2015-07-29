/*global Raven */
define(function() {
    'use strict';

    var tierMapping = {
        '/join/supporter/thankyou': 568467,
        '/join/partner/thankyou': 568468,
        '/join/patron/thankyou': 568469
    };

    function load() {
        var tierId = tierMapping[window.location.pathname] || false;
        var scriptUrl;
        // Specific page tracking if we match a given path
        if(tierId) {
            scriptUrl = '//secure.adnxs.com/px?id=' + tierId + '&t=1';
            require('js!' + scriptUrl).then(null, function(err) {
                Raven.captureException(err);
            });
        }
    }

    return {
        load: load
    };
});
