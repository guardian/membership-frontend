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
    var Upgrade = function () {
        self = this;
    };

    component.define(Upgrade);

    Upgrade.prototype.classes = {
        STRIPE_FORM: 'js-stripe-form',
        ADDRESS_LINE_ONE: 'js-address-line-one',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        BILLING: 'js-toggle-billing-address',
        BILLING_CTA: 'js-toggle-billing-address-cta',
        BILLING_FIELDSET: 'js-billingAddress-fieldset',
        FORM_FIELD: 'form-field'
    };

    Upgrade.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Upgrade.prototype.init = function () {
        this.setupForm();
        this.toggleBillingAddressListener();

        this.setupDeliveryToggleState();
        this.setupPasswordStrength();
    };

    Upgrade.prototype.toggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();
        this.setupBillingToggleState();

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

    Upgrade.prototype.detachElements = function($elements) {
        for (var i = 0, $elementsLength = $elements.length; i < $elementsLength; i++) {
            var $element = $elements[i];
            if ($element.parent().length !== 0) {
                $element = $element.detach();
            }
        }
    };

    Upgrade.prototype.selectedOptionName = function(optionIndex, selectElementOptions) {
        return selectElementOptions[optionIndex].textContent.toLowerCase();
    };

    Upgrade.prototype.setupDeliveryToggleState = function() {
        this.setupToggleState(
            $('#country-deliveryAddress', this.form.formElement),
            $('#county-deliveryAddress', this.form.formElement),
            $('#state-deliveryAddress', this.form.formElement),
            $('#province-deliveryAddress', this.form.formElement)
        );
    };

    Upgrade.prototype.setupBillingToggleState = function() {
        this.setupToggleState(
            $('#country-billingAddress', this.form.formElement),
            $('#county-billingAddress', this.form.formElement),
            $('#state-billingAddress', this.form.formElement),
            $('#province-billingAddress', this.form.formElement)
        );
    };

    Upgrade.prototype.setupToggleState = function($countrySelect, $countySelect, $stateSelect, $provinceSelect) {

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

    return Upgrade;
});
