/*global Raven, twttr */
define(function() {
    'use strict';

    var tierMapping = {
        '/join/friend/thankyou': 'l6gt9',
        '/join/supporter/thankyou': 'l6gtb',
        '/join/partner/thankyou': 'l6gta',
        '/join/patron/thankyou': 'l6gtc'
    };

    function load() {
        var scriptUrl = '//platform.twitter.com/oct.js';
        var tierId = tierMapping[window.location.pathname] || false;
        require('js!' + scriptUrl).then(function() {
            // Default tracking for all pageviews
            twttr.conversion.trackPid('l6gt8', { tw_sale_amount: 0, tw_order_quantity: 0 });
            // Specific page tracking if we match a given path
            if(tierId) {
                twttr.conversion.trackPid(tierId, { tw_sale_amount: 0, tw_order_quantity: 0 });
            }
        }, function(err) {
            Raven.captureException(err);
        });
    }

    return {
        load: load
    };
});
