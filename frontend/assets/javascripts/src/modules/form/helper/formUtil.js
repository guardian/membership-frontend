define([
    'src/utils/helper',
    'src/utils/url',
    'ophan-tracker-js/build/ophan.membership',
], function (utilsHelper, url, ophan) {
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

    function hasPaypal () {

        var platform = url.getQueryParameterByName('platform');
        var campaignCode = url.getQueryParameterByName('INTCMP');
        var appCampCodes = [
            'APP_ANDROID_MEMBERSHIP_PAYMENT_SCREEN',
            'APP_IOS_MEMBERSHIP_PAYMENT_SCREEN',
            'gdnwb_copts_memco_kr3_app_epic_ask4',
            'gdnwb_copts_memco_app_epic_always_ask'
        ];

        // Platform for new versions of the apps, camp codes for old versions.
        var fromApps = platform === 'ios' || platform === 'android' ||
            appCampCodes.indexOf(campaignCode) > -1;

        return !!document.getElementById('paypal-button-checkout') && !fromApps;

    }

    function hasStripeCheckout () {
        return !!document.querySelector('.js-stripe-checkout');
    }

    function hasAccordion () {
        return !!document.querySelector('.js-continue-name-address');
    }

    function hasEmailInput () {
        return !!document.getElementById('form-field__error-message-email-checker') && (document.getElementById('form-field__error-message-email-checker') instanceof HTMLParagraphElement);
    }

    function attachOphanPageviewId() {
        var input = getFormElem().querySelector('.js-ophan-pageview-id')
        input.setAttribute('value', ophan.viewId);
    }

    /**
     * formUtil singleton provides:
     *    elem: DomElement - the form element
     *    elems: Array - form elements that need validation
     *    errs: Array[String] - current form errors
     *    flush: enables elem and elems flushing for add/remove of validation
     *
     * @returns {{elem, elems, errs: Array, flush: flush}}
     */
    var formUtil = function () {
        var form = getFormElem();

        if (form) {
            return {
                elem: form,
                elems: getInputsToValidate(form.elements),
                hasAccordion: hasAccordion(),
                hasPaypal: hasPaypal(),
                hasStripeCheckout: hasStripeCheckout(),
                hasEmailInput: hasEmailInput(),
                attachOphanPageviewId: attachOphanPageviewId,
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
