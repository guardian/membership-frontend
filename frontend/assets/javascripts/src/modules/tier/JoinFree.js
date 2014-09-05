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
    var JoinFree = function () {
        self = this;
    };

    component.define(JoinFree);

    JoinFree.prototype.classes = {
        NAME_FIRST: 'js-name-first',
        NAME_LAST: 'js-name-last',
        ADDRESS_FORM: 'js-address-form',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        COUNTY_CONTAINER_DELIVERY: 'js-county-container-deliveryAddress',
        POSTCODE_LABEL_DELIVERY: 'js-postcode-deliveryAddress',
        FORM_FIELD: 'form-field'
    };

    JoinFree.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinFree.prototype.init = function () {
        this.setupForm();
        this.setupPasswordStrength();
        this.setupDeliveryToggleState();
    };


    JoinFree.prototype.detachElements = function($elements) {
        for (var i = 0, $elementsLength = $elements.length; i < $elementsLength; i++) {
            var $element = $elements[i];
            if ($element.parent().length !== 0) {
                $element = $element.detach();
            }
        }
    };

    JoinFree.prototype.selectedOptionName = function(optionIndex, selectElementOptions) {
        return selectElementOptions[optionIndex].textContent.toLowerCase();
    };

    JoinFree.prototype.setupDeliveryToggleState = function() {
        this.setupToggleState(
            $('#country-deliveryAddress', this.form.formElement),
            $('#county-or-state-deliveryAddress', this.form.formElement),
            $('#state-deliveryAddress', this.form.formElement),
            $('#province-deliveryAddress', this.form.formElement),
            $(this.getClass('COUNTY_CONTAINER_DELIVERY'), this.form.formElement),
            $(this.getClass('POSTCODE_LABEL_DELIVERY'), this.form.formElement)
        );
    };

    JoinFree.prototype.setupToggleState = function(
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

    JoinFree.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('ADDRESS_FORM');
        this.form = new Form(formElement);
        this.form.init();
    };

    JoinFree.prototype.setupPasswordStrength = function () {
        (new Password()).init();
    };

    return JoinFree;
});
