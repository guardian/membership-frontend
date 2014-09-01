define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form',
    'src/utils/form/Password'
], function ($, bean, component, Form, Password) {
    'use strict';

    var self;
    var Upgrade = function () {
        self = this;
    };

    component.define(Upgrade);

    Upgrade.prototype.classes = {
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

    Upgrade.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Upgrade.prototype.init = function () {
        this.setupForm();
        this.toggleBillingAddressListener();
        this.setupPasswordStrength();
    };

    Upgrade.prototype.toggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();

        var $billing = $(this.getElem('BILLING')).removeClass('u-h');
        var $billingDetails = $(this.getElem('BILLING_FIELDSET')).detach();
        var $billingCTA = $(this.getElem('BILLING_CTA'));

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

    Upgrade.prototype.addValidatorFromValidationProfile = function () {

        this.form.addValidatorFromValidationProfile(
            [
                {
                    elem: $(this.getClass('ADDRESS_LINE_ONE'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('TOWN'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('POST_CODE'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                }
            ]);
    };

    Upgrade.prototype.removeValidatorFromValidationProfile = function () {

        this.form.removeValidatorFromValidationProfile(
            [
                {
                    elem: $(this.getClass('ADDRESS_LINE_ONE'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('TOWN'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                },
                {
                    elem: $(this.getClass('POST_CODE'), this.getClass('BILLING_FIELDSET'))[0],
                    validator: 'requiredValidator'
                }
            ]);
    };

    Upgrade.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('STRIPE_FORM'),
            changeToTier = formElement.getAttribute('data-change-to-tier').toLowerCase();
        this.form = new Form(formElement,  '/tier/change/' + changeToTier, '/tier/change/' + changeToTier + '/summary');
        this.form.init();
    };

    Upgrade.prototype.setupPasswordStrength = function () {
        (new Password()).init();
    };

    return Upgrade;
});
