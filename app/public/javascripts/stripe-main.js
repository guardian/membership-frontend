/*global require */

require.config({
    paths: {
        // 'config': '../app/config',
        'jquery': '//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min',
        'payment': 'lib/jquery.payment',
        'stripe': 'https://js.stripe.com/v2/?'
    },
    shim: {
        'payment': {
            deps: ['jquery']
        }
    }
});

require([
    'jquery',
    'payment',
    'stripe'
], function($, Payment, Stripe) {

    "use strict";

    var init = function () {
        console.log($, $.payment, Stripe);
    };

    $(init);
});
