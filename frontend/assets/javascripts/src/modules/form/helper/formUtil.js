define([
    'src/utils/helper',
    'src/utils/url'
], function (utilsHelper, url) {
    'use strict';

    /**
     * Filter only the elements that require validation, 'input', 'textarea', 'select' input elements that are
     * required or have a validation profile
     * @param elems
     * @returns {*}
     */
    var inputsToValidate = function (elems) {
        return elems.filter(function (elem) {
            var nodeName = elem.nodeName.toLowerCase();
            var inputTypes = ['input', 'textarea', 'select'];
            var required = elem.hasAttribute('required');
            var validationProfile = elem.getAttribute('data-validation');

            return inputTypes.indexOf(nodeName) !== -1 && (required || validationProfile);
        });
    };

    var  getFormElem = function () {
        return document.querySelector('.js-form');
    };

    var getInputsToValidate = function (elems) {
        return inputsToValidate(utilsHelper.toArray(elems));
    };

    var hasPayment = function () {
        return !!document.querySelector('.js-credit-card-number');
    };

    function hasPaypal () {

        var platform = url.getQueryParameterByName('platform');
        var campaignCode = url.getQueryParameterByName('INTCMP');
        var androidCampCodes = [
            'APP_ANDROID_MEMBERSHIP_PAYMENT_SCREEN',
            'gdnwb_copts_memco_kr3_app_epic_ask4',
            'gdnwb_copts_memco_app_epic_always_ask'
        ];

        var fromApps = platform === 'ios' || platform === 'android' ||
            androidCampCodes.indexOf(campaignCode) > -1;

        return !!document.getElementById('paypal-button-checkout') && !fromApps;

    }

    function hasStripeCheckout () {
        return !!document.querySelector('.js-stripe-checkout');
    }

    function hasAccordion () {
        return !!document.querySelector('.js-continue-name-address');
    }

    /**
     * formUtil singleton provides:
     *    elem: DomElement - the form element
     *    elems: Array - form elements that need validation
     *    hasPayment: Boolean - does the form has payment facilities
     *    errs: Array[String] - current form errors
     *    flush: enables elem and elems flushing for add/remove of validation
     *
     * @returns {{elem, elems, hasPayment, errs: Array, flush: flush}}
     */
    var formUtil = function () {
        var form = getFormElem();

        if (form) {
            return {
                elem: form,
                elems: getInputsToValidate(form.elements),
                hasAccordion: hasAccordion(),
                hasPayment: hasPayment(),
                hasPaypal: hasPaypal(),
                hasStripeCheckout: hasStripeCheckout(),
                errs: [],
                flush: function () {
                    this.elem = getFormElem();
                    this.elems = getInputsToValidate(this.elem.elements);
                }
            };
        }
    };

    return formUtil();
});
