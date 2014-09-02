define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form',
    'src/utils/form/Password'
], function ($, bean, component, Form, Password) {
    'use strict';

    var self;
    var JoinPaid = function() {
        self = this;
    };

    component.define(JoinPaid);

    JoinPaid.prototype.classes = {
        NAME_FIRST: 'js-name-first',
        NAME_LAST: 'js-name-last',
        STRIPE_FORM: 'js-stripe-form',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        ADDRESS_LINE_ONE: 'js-address-line-one',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        BILLING: 'js-toggle-billing-address',
        BILLING_CTA: 'js-toggle-billing-address-cta',
        BILLING_FIELDSET: 'js-billingAddress-fieldset'
    };

    JoinPaid.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinPaid.prototype.init = function () {
        this.setupForm();
        this.toggleBillingAddressListener();
        this.setupPasswordStrength();
    };

    JoinPaid.prototype.toggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();

        var $billing = $(this.getClass('BILLING'), this.form.formElement).removeClass('u-h');
        var $billingDetails = $(this.getClass('BILLING_FIELDSET'), this.form.formElement).detach();
        var $billingCTA = $(this.getClass('BILLING_CTA'), this.form.formElement);

        bean.on($billingCTA[0], 'click', function () {

            if ($billingDetails.parent().length === 0) {
                // open
                $billingDetails.insertAfter($billing);
                $billingCTA.text('Same billing address as above');
                self.addValidatorFromValidationProfile();
            } else {
                // closed
                $('.form-field', $billingDetails).removeClass('form-field--error');
                $('.form-field__error-message', $billingDetails).remove();

                $billingCTA.text('Different billing address?');
                self.removeValidatorFromValidationProfile();
                $billingDetails.detach();
            }
        });
    };

    JoinPaid.prototype.addValidatorFromValidationProfile = function () {

        this.form.addValidatorFromValidationProfile(
            [
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('ADDRESS_LINE_ONE'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('TOWN'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('POST_CODE'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                }
            ]);
    };

    JoinPaid.prototype.removeValidatorFromValidationProfile = function () {

        this.form.removeValidatorFromValidationProfile(
            [
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('ADDRESS_LINE_ONE'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('TOWN'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('BILLING_FIELDSET') + ' ' + this.getClass('POST_CODE'), this.form.formElement)[0],
                    validator: 'requiredValidator'
                }
            ]);
    };

    JoinPaid.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('STRIPE_FORM');
        this.form = new Form(formElement, '/subscription/subscribe', window.location.href.replace('enter-details', 'thankyou'));
        this.form.init();
    };

    JoinPaid.prototype.setupPasswordStrength = function () {
        (new Password()).init();
    };

    return JoinPaid;
});
