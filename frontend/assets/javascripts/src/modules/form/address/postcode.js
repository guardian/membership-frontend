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
     * toggle postcode validation dependant on country selected
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
     * toggle address validation dependant on country selected
     * @param context
     * @param optionTxt
     */
    var toggleAddressValidation = function (context, optionTxt) {
        var $addressLineOneInput = $(ADDRESS_LINE_ONE_INPUT_SELECTOR, context);
        var $townInput = $(TOWN_INPUT_SELECTOR, context);

        // if we are on a form that doesn't have address validation by default toggle it
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
