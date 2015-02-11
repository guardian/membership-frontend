define([
    'src/modules/form/payment/validationProfiles',
    'src/modules/form/validation/display'
], function (validationProfiles, display) {
    'use strict';

    var DATA_VALIDATION_ATTRIBUTE_NAME = 'data-validation';

    /**
     * check validity of elem and toggle error state
     * @param elem
     */
    var check = function (elem) {
        display.toggleErrorState({
            isValid: testValidity(elem),
            elem: elem
        });
    };

    /**
     * test validity dependant on elem attributes
     *  - require
     *  - minlength
     *  - maxlength
     *  - pattern
     *  - data-validation="<validation profile>"
     *
     * @param elem
     * @returns {boolean}
     */
    var testValidity = function (elem) {
        var valid = true;

        if (elem.hasAttribute('disabled')) { return true; }

        if (elem.getAttribute(DATA_VALIDATION_ATTRIBUTE_NAME)) {
            valid = valid && profileValidation(elem);
        }

        valid = valid && requiredValidation(elem);
        valid = valid && lengthValidation(elem);
        valid = valid && patternValidation(elem);

        return valid;
    };

    /**
     * If elem has the required attribute it must have a value
     * @param elem
     * @returns {*|boolean}
     */
    var requiredValidation = function (elem) {
        return !elem.hasAttribute('required') || elem.value !== '';
    };

    /**
     * if elem has the minlength, maxlength attributes check value length is within these bounds
     * @param elem
     * @returns {*|boolean}
     */
    var lengthValidation = function (elem) {
        var value = elem.value;
        var minLength = elem.getAttribute('minlength');
        var maxLength = elem.getAttribute('maxlength');
        return (!minLength || value.length >= minLength) && (!maxLength || value.length <= maxLength);
    };

    /**
     * if the elem has a pattern check it
     * @param elem
     * @returns {*}
     */
    var patternValidation = function (elem) {
        var pattern = elem.getAttribute('pattern');

        return !pattern || new RegExp(pattern).test(elem.value);
    };

    /**
     * if the elem has a validation profile and this is a validation profile we hold then validate the elem against it
     * @param valid
     * @param elem
     * @returns {*}
     */
    var profileValidation = function (elem) {
        var profile = elem.getAttribute(DATA_VALIDATION_ATTRIBUTE_NAME);
        return validationProfileExists(profile) && validationProfiles[profile](elem);
    };

    var validationProfileExists = function (profile) {
        return validationProfiles[profile] && typeof validationProfiles[profile] === 'function';
    };

    return {
        check: check,
        testValidity: testValidity
    };
});
