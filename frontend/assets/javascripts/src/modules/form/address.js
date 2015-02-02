define([
    'src/modules/form/address/billing',
    'src/modules/form/address/rules'
], function (billingAddress, address) {
    'use strict';

    var BILLING_CTA_ELEM = document.querySelector('.js-toggle-billing-address');
    var BILLING_FIELDSET_ELEM = document.querySelector('.js-billingAddress-fieldset');
    var DELIVERY_FIELDSET_ELEM = document.querySelector('.js-deliveryAddress-fieldset');

    /**
     * Initialise address
     * Add toggle billing address listeners
     * Setup rules and listeners for showing specific subdivision (state/province/county) on country change
     * Setup rules and listeners for address validation on country change
     */
    var init = function () {
        if (DELIVERY_FIELDSET_ELEM) {
            address.addRules(DELIVERY_FIELDSET_ELEM);
        }

        if (BILLING_CTA_ELEM) {
            billingAddress.init();
            address.addRules(BILLING_FIELDSET_ELEM);
        }
    };

    return {
        init: init
    };

});
