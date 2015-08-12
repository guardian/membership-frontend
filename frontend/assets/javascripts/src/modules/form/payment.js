define([
    'src/modules/form/payment/listeners',
    'src/modules/form/payment/options'
], function (listeners, options) {
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
        options.init();
    };

    return {
        init: init
    };
});
