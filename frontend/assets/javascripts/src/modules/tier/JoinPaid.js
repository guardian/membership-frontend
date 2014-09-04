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

    JoinPaid.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinPaid.prototype.init = function () {
        this.setupForm();
        this.setupToggleBillingAddressListener();
        this.setupDeliveryToggleState();
        this.setupPasswordStrength();
        this.setupCtaPaymentOptionPriceListeners();
        this.setupCvcToggle();
    };

    JoinPaid.prototype.setupToggleBillingAddressListener = function() {
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
            $('#province-deliveryAddress', this.form.formElement),
            $(this.getClass('COUNTY_CONTAINER_DELIVERY'), this.form.formElement),
            $(this.getClass('POSTCODE_LABEL_DELIVERY'), this.form.formElement)
        );
    };

    JoinPaid.prototype.setupBillingToggleState = function() {
        var billingFieldset = $(this.getElem('BILLING_FIELDSET'));
        this.setupToggleState(
            $('#country-billingAddress', billingFieldset),
            $('#county-or-state-billingAddress', billingFieldset),
            $('#state-billingAddress', billingFieldset),
            $('#province-billingAddress', billingFieldset),
            $(this.getClass('COUNTY_CONTAINER_BILLING'), billingFieldset),
            $(this.getClass('POSTCODE_LABEL_BILLING'), billingFieldset)
        );
    };

    JoinPaid.prototype.setupToggleState = function(
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

    JoinPaid.prototype.setupCtaPaymentOptionPriceListeners = function() {
        var $paymentOptionsContainer = $(this.getElem('PAYMENT_OPTIONS_CONTAINER'));
        var $ctaOptionPrice = $(this.getElem('CTA_PAYMENT_OPTION_PRICE'));

        bean.on($paymentOptionsContainer[0], 'click', 'input', function (e) {
            var input = e && e.target;
            var paymentOptionPrice = input.getAttribute('data-pricing-option-amount');
            $ctaOptionPrice.text(paymentOptionPrice);
            self.populateCardNote(input.value);
        });
    };

    JoinPaid.prototype.populateCardNote = function(period) {
        var $cardNote = $(this.getElem('CARD_DETAILS_NOTE'));
        var $cardNoteCharge = $(this.getClass('CARD_NOTE_CHARGE'), $cardNote);
        var $cardNotePeriod = $(this.getClass('CARD_NOTE_PERIOD'), $cardNote);
        var $cardNotePaymentTaken = $(this.getClass('CARD_NOTE_PAYMENT_TAKEN'), $cardNote);

        $cardNoteCharge.text($cardNoteCharge.attr('data-' + period));
        $cardNotePeriod.text($cardNotePeriod.attr('data-' + period));
        $cardNotePaymentTaken.html($cardNotePaymentTaken.attr('data-' + period));
    };


    JoinPaid.prototype.setupCvcToggle = function() {
        var $cvcDescriptionContainer = $(this.getElem('CVC_DESCRIPTION_CONTAINER'));
        var $cvcCta = $(this.getElem('CVC_CTA'));

        bean.on($cvcCta[0], 'click', function (e) {
            e.preventDefault();
            $cvcDescriptionContainer.toggleClass('u-h');
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
