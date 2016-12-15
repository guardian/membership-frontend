/*global Stripe*/
define([
    'src/modules/form/payment/listeners'
], function (listeners) {
    'use strict';

    /**
     * Initialise payment
     * Setup stripe key
     * Add card masker listener
     * Add card input image listener
     * Setup Card detail options text
     */
    var init = function () {
        Stripe.setPublishableKey(guardian.stripePublicKey);
        listeners.addPaymentListeners();
    };

    return {
        init: init
    };
});
