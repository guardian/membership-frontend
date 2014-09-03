define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form',
    'src/utils/form/Password',
    'src/utils/helper'
], function ($, bean, component, Form, Password, helper) {
    'use strict';

    var UNITED_STATES_STRING = 'united states';
    var CANADA_STRING = 'canada';
    var self;
    var JoinPaid = function() {
        self = this;
    };

    component.define(JoinPaid);

    JoinPaid.prototype.classes = {
        STRIPE_FORM: 'js-stripe-form',
        ADDRESS_LINE_ONE: 'js-address-line-one',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        BILLING: 'js-toggle-billing-address',
        BILLING_CTA: 'js-toggle-billing-address-cta',
        BILLING_FIELDSET: 'js-billingAddress-fieldset',
        FORM_FIELD: 'form-field'
    };

    JoinPaid.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinPaid.prototype.init = function () {
        this.setupForm();
        this.setupToggleBillingAddressListener();
        this.setupDeliveryToggleState();
        this.setupPasswordStrength();
    };

    JoinPaid.prototype.setupToggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();
        this.setupBillingToggleState();

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

    JoinPaid.prototype.detachElements = function($elements) {
        for (var i = 0, $elementsLength = $elements.length; i < $elementsLength; i++) {
            var $element = $elements[i];
            if ($element.parent().length !== 0) {
                $element = $element.detach();
            }
        }
    };

    JoinPaid.prototype.selectedOptionName = function(optionIndex, selectElementOptions) {
        return selectElementOptions[optionIndex].textContent.toLowerCase();
    };

    JoinPaid.prototype.setupDeliveryToggleState = function() {
        this.setupToggleState(
            $('#country-deliveryAddress', this.form.formElement),
            $('#county-or-state-deliveryAddress', this.form.formElement),
            $('#state-deliveryAddress', this.form.formElement),
            $('#province-deliveryAddress', this.form.formElement)
        );
    };

    JoinPaid.prototype.setupBillingToggleState = function() {
        this.setupToggleState(
            $('#country-billingAddress', this.form.formElement),
            $('#county-or-state-billingAddress', this.form.formElement),
            $('#state-billingAddress', this.form.formElement),
            $('#province-billingAddress', this.form.formElement)
        );
    };

    JoinPaid.prototype.setupToggleState = function($countrySelect, $countySelect, $stateSelect, $provinceSelect) {

        var formFieldClass = this.getClass('FORM_FIELD', true);
        var $selectElements = [];
        var $countrySelectParent = helper.getSpecifiedParent($countrySelect, formFieldClass);
        var $countySelectParent = helper.getSpecifiedParent($countySelect, formFieldClass);
        var $usaStateSelectParent = helper.getSpecifiedParent($stateSelect, formFieldClass).detach();
        var $canadaProvinceSelectParent = helper.getSpecifiedParent($provinceSelect, formFieldClass).detach();

        $selectElements.push($countySelectParent, $usaStateSelectParent, $canadaProvinceSelectParent);

        bean.on($countrySelect[0], 'change', function (e) {

            var optionIndex = e && e.target.selectedIndex;
            var selectedName = self.selectedOptionName(optionIndex, $countrySelect[0].options);

            self.detachElements($selectElements);

            if (selectedName === UNITED_STATES_STRING) {
                $usaStateSelectParent.removeClass('u-h').insertAfter($countrySelectParent);
            } else if (selectedName === CANADA_STRING) {
                $canadaProvinceSelectParent.removeClass('u-h').insertAfter($countrySelectParent);
            } else {
                $countySelectParent.removeClass('u-h').insertAfter($countrySelectParent);
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
