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
    var ZIP_CODE = 'Zip code';
    var POST_CODE = 'Post code';
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
        BILLING_ADDRESS_OPTION_CONTAINER: 'js-billing-address-container',
        BILLING_FIELDSET: 'js-billingAddress-fieldset',
        FORM_FIELD: 'form-field',
        COUNTY_CONTAINER_DELIVERY: 'js-county-container-deliveryAddress',
        COUNTY_CONTAINER_BILLING: 'js-county-container-billingAddress',
        POSTCODE_LABEL_DELIVERY: 'js-postcode-deliveryAddress',
        POSTCODE_LABEL_BILLING: 'js-postcode-billingAddress',
        CTA_PAYMENT_OPTION_PRICE: 'js-cta-payment-option-price',
        PAYMENT_OPTIONS_CONTAINER: 'js-payment-options-container',
        CVC_CTA: 'js-cvc-cta',
        CVC_DESCRIPTION_CONTAINER: 'js-cvc-image-container',
        CARD_DETAILS_NOTE: 'js-card-details-note',
        CARD_NOTE_CHARGE: 'js-card-note-pricing-charge',
        CARD_NOTE_PERIOD: 'js-card-note-pricing-period',
        CARD_NOTE_PAYMENT_TAKEN: 'js-card-note-payment-taken'
    };

    Upgrade.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Upgrade.prototype.init = function () {
        this.setupForm();
        this.setupToggleBillingAddressListener();
        this.setupDeliveryToggleState();
        this.setupCtaPaymentOptionPriceListeners();
        this.setupCvcToggle();
    };

    Upgrade.prototype.setupToggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();
        this.setupBillingToggleState();

        var $billing = $(this.getClass('BILLING'), this.form.formElement).removeClass('u-h');
        var $billingDetails = $(this.getClass('BILLING_FIELDSET'), this.form.formElement).addClass('fieldset--no-top-border').detach();
        var $billingOptionsContainer = $(this.getClass('BILLING_ADDRESS_OPTION_CONTAINER'), this.form.formElement);

        bean.on($billingOptionsContainer[0], 'click', 'input', function () {

            if ($billingDetails.parent().length === 0) {
                // open
                $billingDetails.insertAfter($billing);
                self.addValidatorFromValidationProfile();
            } else {
                // closed
                $('.form-field', $billingDetails).removeClass('form-field--error');
                $('.form-field__error-message', $billingDetails).remove();

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
            $('#county-or-state-deliveryAddress', this.form.formElement),
            $('#state-deliveryAddress', this.form.formElement),
            $('#province-deliveryAddress', this.form.formElement),
            $(this.getClass('COUNTY_CONTAINER_DELIVERY'), this.form.formElement),
            $(this.getClass('POSTCODE_LABEL_DELIVERY'), this.form.formElement)
        );
    };

    Upgrade.prototype.setupBillingToggleState = function() {
        var billingFieldset = $(this.getClass('BILLING_FIELDSET'), this.form.formElement);
        this.setupToggleState(
            $('#country-billingAddress', billingFieldset),
            $('#county-or-state-billingAddress', billingFieldset),
            $('#state-billingAddress', billingFieldset),
            $('#province-billingAddress', billingFieldset),
            $(this.getClass('COUNTY_CONTAINER_BILLING'), billingFieldset),
            $(this.getClass('POSTCODE_LABEL_BILLING'), billingFieldset)
        );
    };

    Upgrade.prototype.setupToggleState = function(
        $countrySelect, $countySelect, $stateSelect, $provinceSelect, $countyContainer, $postcodeLabel) {

        var formFieldClass = this.getClass('FORM_FIELD', true);
        var $selectElements = [];
        var $countySelectParent = helper.getSpecifiedParent($countySelect, formFieldClass);
        var $usaStateSelectParent = helper.getSpecifiedParent($stateSelect, formFieldClass).detach();
        var $canadaProvinceSelectParent = helper.getSpecifiedParent($provinceSelect, formFieldClass).detach();

        $selectElements.push($countySelectParent, $usaStateSelectParent, $canadaProvinceSelectParent);

        bean.on($countrySelect[0], 'change', function (e) {

            var optionIndex = e && e.target.selectedIndex;
            var selectedName = self.selectedOptionName(optionIndex, $countrySelect[0].options);

            self.detachElements($selectElements);

            if (selectedName === UNITED_STATES_STRING) {
                $countyContainer.append($usaStateSelectParent.removeClass('u-h'));
                $postcodeLabel.text(ZIP_CODE);
            } else if (selectedName === CANADA_STRING) {
                $countyContainer.append($canadaProvinceSelectParent.removeClass('u-h'));
                $postcodeLabel.text(ZIP_CODE);
            } else {
                $countyContainer.append($countySelectParent.removeClass('u-h'));
                $postcodeLabel.text(POST_CODE);
            }
        });
    };

    Upgrade.prototype.setupCtaPaymentOptionPriceListeners = function() {
        var $paymentOptionsContainer = $(this.getClass('PAYMENT_OPTIONS_CONTAINER'), this.form.formElement);
        var $ctaOptionPrice = $(this.getClass('CTA_PAYMENT_OPTION_PRICE'), this.form.formElement);

        bean.on($paymentOptionsContainer[0], 'click', 'input', function (e) {
            var input = e && e.target;
            var paymentOptionPrice = input.getAttribute('data-pricing-option-amount');
            $ctaOptionPrice.text(paymentOptionPrice);
            self.populateCardNote(input.value);
        });
    };

    Upgrade.prototype.populateCardNote = function(period) {
        var $cardNote = $(this.getElem('CARD_DETAILS_NOTE'));
        var $cardNoteCharge = $(this.getClass('CARD_NOTE_CHARGE'), $cardNote);
        var $cardNotePeriod = $(this.getClass('CARD_NOTE_PERIOD'), $cardNote);
        var $cardNotePaymentTaken = $(this.getClass('CARD_NOTE_PAYMENT_TAKEN'), $cardNote);

        $cardNoteCharge.text($cardNoteCharge.attr('data-' + period));
        $cardNotePeriod.text($cardNotePeriod.attr('data-' + period));
        $cardNotePaymentTaken.html($cardNotePaymentTaken.attr('data-' + period));
    };


    Upgrade.prototype.setupCvcToggle = function() {
        var $cvcDescriptionContainer = $(this.getClass('CVC_DESCRIPTION_CONTAINER'), this.form.formElement);
        var $cvcCta = $(this.getClass('CVC_CTA'), this.form.formElement);

        bean.on($cvcCta[0], 'click', function (e) {
            e.preventDefault();
            $cvcDescriptionContainer.toggleClass('u-h');
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
