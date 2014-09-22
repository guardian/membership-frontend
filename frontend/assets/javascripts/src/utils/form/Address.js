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

    var Address = function(formElement) {
        self = this;
        this.form = formElement;
    };

    component.define(Address);

    Address.prototype.classes = {
        ADDRESS_LINE_ONE: 'js-address-line-one',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        BILLING: 'js-toggle-billing-address',
        BILLING_ADDRESS_INPUTS: 'js-billing-input',
        BILLING_FIELDSET: 'js-billingAddress-fieldset',
        FORM_FIELD: 'form-field',
        COUNTY_CONTAINER_DELIVERY: 'js-county-container-deliveryAddress',
        COUNTY_CONTAINER_BILLING: 'js-county-container-billingAddress',
        POSTCODE_LABEL_DELIVERY: 'js-postcode-deliveryAddress',
        POSTCODE_LABEL_BILLING: 'js-postcode-billingAddress',
        COUNTRY_DELIVERY_ADDRESS: 'js-country-deliveryAddress',
        COUNTY_OR_STATE_DELIVERY_ADDRESS: 'js-county-or-state-deliveryAddress',
        STATE_DELIVERY_ADDRESS: 'js-state-deliveryAddress',
        PROVINCE_DELIVERY_ADDRESS: 'js-province-deliveryAddress',
        COUNTRY_BILLING_ADDRESS: 'js-country-billingAddress',
        COUNTY_OR_STATE_BILLING_ADDRESS: 'js-county-or-state-billingAddress',
        STATE_BILLING_ADDRESS: 'js-state-billingAddress',
        PROVINCE_BILLING_ADDRESS: 'js-province-billingAddress'
    };

    Address.prototype.setupToggleBillingAddressListener = function() {
        this.removeValidatorFromValidationProfile();
        this.setupBillingToggleState();

        var $billing = $(this.getClass('BILLING'), this.form.formElement).removeClass('u-h');
        var $billingDetails = $(this.getClass('BILLING_FIELDSET'), this.form.formElement).addClass('fieldset--no-top-border').detach();
        var $billingInputs = $(this.getClass('BILLING_ADDRESS_INPUTS'), this.form.formElement);
        var toggleBillingDeliveryDetails = function () {
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
        };

        //made like this for testing purposes, its tricky to test delegate events
        $billingInputs.each(function (input) {
            bean.on(input, 'click', toggleBillingDeliveryDetails);
        });
    };

    Address.prototype.selectedOptionName = function(optionIndex, selectElementOptions) {
        return selectElementOptions[optionIndex].textContent.toLowerCase();
    };

    Address.prototype.setupDeliveryToggleState = function() {
        this.setupToggleState(
            $(this.getClass('COUNTRY_DELIVERY_ADDRESS'), this.form.formElement),
            $(this.getClass('COUNTY_OR_STATE_DELIVERY_ADDRESS'), this.form.formElement),
            $(this.getClass('STATE_DELIVERY_ADDRESS'), this.form.formElement),
            $(this.getClass('PROVINCE_DELIVERY_ADDRESS'), this.form.formElement),
            $(this.getClass('COUNTY_CONTAINER_DELIVERY'), this.form.formElement),
            $(this.getClass('POSTCODE_LABEL_DELIVERY'), this.form.formElement)
        );
    };

    Address.prototype.setupBillingToggleState = function() {
        var billingFieldset = $(this.getClass('BILLING_FIELDSET'), this.form.formElement);
        this.setupToggleState(
            $(this.getClass('COUNTRY_BILLING_ADDRESS'), billingFieldset),
            $(this.getClass('COUNTY_OR_STATE_BILLING_ADDRESS'), billingFieldset),
            $(this.getClass('STATE_BILLING_ADDRESS'), billingFieldset),
            $(this.getClass('PROVINCE_BILLING_ADDRESS'), billingFieldset),
            $(this.getClass('COUNTY_CONTAINER_BILLING'), billingFieldset),
            $(this.getClass('POSTCODE_LABEL_BILLING'), billingFieldset)
        );
    };

    Address.prototype.setupToggleState = function(
        $countrySelect, $countySelect, $stateSelect, $provinceSelect, $countyContainer, $postcodeLabel) {

        var formFieldClass = this.getClass('FORM_FIELD', true);
        var $countySelectParent = helper.getSpecifiedParent($countySelect, formFieldClass);
        var $usaStateSelectParent = helper.getSpecifiedParent($stateSelect, formFieldClass).detach();
        var $canadaProvinceSelectParent = helper.getSpecifiedParent($provinceSelect, formFieldClass).detach();
        var $selectElements = $($countySelectParent, $usaStateSelectParent, $canadaProvinceSelectParent);
        var toggleSelectElements = function (selectedItemName) {
            $selectElements.detach();

            if (selectedItemName === UNITED_STATES_STRING) {
                $countyContainer.append($usaStateSelectParent.removeClass('u-h'));
                $postcodeLabel.text(ZIP_CODE);
            } else if (selectedItemName === CANADA_STRING) {
                $countyContainer.append($canadaProvinceSelectParent.removeClass('u-h'));
                $postcodeLabel.text(ZIP_CODE);
            } else {
                $countyContainer.append($countySelectParent.removeClass('u-h'));
                $postcodeLabel.text(POST_CODE);
            }
        };
        // on page load correctly display country and relevant fields
        var onLoadSelectedItemName = self.selectedOptionName($countrySelect[0].selectedIndex, $countrySelect[0].options);

        toggleSelectElements(onLoadSelectedItemName);

        bean.on($countrySelect[0], 'change', function (e) {
            var optionIndex = e && e.target.selectedIndex;
            var selectedItemName = self.selectedOptionName(optionIndex, $countrySelect[0].options);

            toggleSelectElements(selectedItemName);
        });
    };

    // TODO look into a better way of adding/removing validation profiles
    Address.prototype.getBillingAddressValidationProfiles = function () {
        return [
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
        ];
    };

    Address.prototype.addValidatorFromValidationProfile = function () {
        this.form.addValidatorFromValidationProfile(this.getBillingAddressValidationProfiles());
    };

    Address.prototype.removeValidatorFromValidationProfile = function () {
        this.form.removeValidatorFromValidationProfile(this.getBillingAddressValidationProfiles());
    };

    return Address;
});
