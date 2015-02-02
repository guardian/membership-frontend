/**
 * Utility for setting up the billing address cta and toggle functionality
 */
define([
    '$',
    'bean',
    'src/modules/form/helper/formUtil',
    'src/modules/form/validation/display'
], function ($, bean, form, validationDisplay) {
    'use strict';

    var $BILLING_CTA_ELEM = $('.js-toggle-billing-address');
    var $BILLING_FIELDSET_ELEM = $('.js-billingAddress-fieldset');
    var BILLING_ADDRESS_INPUT_CONTAINER = document.querySelector('.js-billing-input-container');

    var init = function () {
        showCta();
        detachAddress();
        addListeners();
    };

    var showCta = function () {
        $BILLING_CTA_ELEM.removeClass('u-h');
    };

    var detachAddress = function () {
        $BILLING_FIELDSET_ELEM.addClass('fieldset--no-top-border').detach();
        form.flush();
    };

    var addListeners = function () {
        bean.on(BILLING_ADDRESS_INPUT_CONTAINER, 'click', 'input', function (e) {
            toggleBillingAddress(e);
        });
    };

    var hasParent = function (elem) {
        return elem.parentNode;
    };

    var toggleBillingAddress = function (e) {
        var input = e && e.target;
        var showBillingAddress = input.classList.contains('js-use-billing-address');

        if (!hasParent($BILLING_FIELDSET_ELEM[0]) && showBillingAddress) {
            $BILLING_FIELDSET_ELEM.insertAfter($BILLING_CTA_ELEM);
        } else if (hasParent($BILLING_FIELDSET_ELEM[0]) && !showBillingAddress){
            validationDisplay.resetErrorState($('[required]', $BILLING_FIELDSET_ELEM));
            $BILLING_FIELDSET_ELEM.detach();
        }
        form.flush();
    };

    return {
        init: init
    };

});
