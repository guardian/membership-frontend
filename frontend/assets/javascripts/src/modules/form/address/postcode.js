/**
 * This file controls the address line one, town and postcode input validation when you choose Ireland as a country and
 * resets the validation when you change to another country
 */
define([
    '$',
    'src/modules/form/validation/actions'
], function ($, actions) {
    'use strict';

    var IRELAND_STRING = 'ireland';
    var POSTCODE_INPUT_SELECTOR = '.js-postcode';
    var ADDRESS_LINE_ONE_INPUT_SELECTOR = '.js-address-line-one';
    var TOWN_INPUT_SELECTOR = '.js-town';

    var toggle = function (context, optionTxt) {
        togglePostCodeValidation(context, optionTxt);
        toggleAddressValidation(context, optionTxt);
    };

    /**
     * If you choose Ireland as a country remove the postcode validation
     * Reset the validation when you change to a country other than Ireland
     * @param context
     * @param optionTxt
     */
    var togglePostCodeValidation = function (context, optionTxt) {
        var $postcodeInput = $(POSTCODE_INPUT_SELECTOR, context);

        if (optionTxt === IRELAND_STRING) {
            actions.removeValidation([$postcodeInput]);
        } else {
            actions.addValidation([$postcodeInput]);
        }
    };

    /**
     * If we are on a form that does not have address line one and town validation by default then we can toggle it
     * If you choose Ireland as a country add the address line one and town input validation
     * Remove this validation (address line one and town input) when you change to a country other than Ireland
     * @param context
     * @param optionTxt
     */
    var toggleAddressValidation = function (context, optionTxt) {
        var $addressLineOneInput = $(ADDRESS_LINE_ONE_INPUT_SELECTOR, context);
        var $townInput = $(TOWN_INPUT_SELECTOR, context);

        if (!$addressLineOneInput[0].hasAttribute('required') && !$townInput[0].hasAttribute('required')) {
            if (optionTxt === IRELAND_STRING) {
                actions.addValidation([$addressLineOneInput, $townInput]);
            } else {
                actions.removeValidation([$addressLineOneInput, $townInput]);
            }
        }
    };

    return {
        toggle: toggle
    };
});
