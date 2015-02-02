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
            valid = profileValidation(valid, elem);
        }

        valid = requiredValidation(valid, elem);
        valid = lengthValidation(valid, elem);
        valid = patternValidation(valid, elem);

        return valid;
    };

    /**
     * If elem has the required attribute it must have a value
     * @param valid
     * @param elem
     * @returns {*|boolean}
     */
    var requiredValidation = function (valid, elem) {
        return valid && (!elem.hasAttribute('required') || elem.value !== '');
    };

    /**
     * if elem has the minlength, maxlength attributes check value length is within these bounds
     * @param valid
     * @param elem
     * @returns {*|boolean}
     */
    var lengthValidation = function (valid, elem) {
        var value = elem.value;
        var minLength = elem.getAttribute('minlength');
        var maxLength = elem.getAttribute('maxlength');
        return valid && ((!minLength || value.length >= minLength) && (!maxLength || value.length <= maxLength));
    };

    /**
     * if the elem has a pattern check it
     * @param valid
     * @param elem
     * @returns {*}
     */
    var patternValidation = function (valid, elem) {
        var pattern = elem.getAttribute('pattern');
        if (valid && pattern) {
            var regEx = new RegExp(pattern);
            return regEx.text(pattern);
        }

        return valid;
    };

    /**
     * if the elem has a validation profile and this is a validation profile we hold then validate the elem against it
     * @param valid
     * @param elem
     * @returns {*}
     */
    var profileValidation = function (valid, elem) {
        var profile = elem.getAttribute(DATA_VALIDATION_ATTRIBUTE_NAME);
        return valid && (validationProfileExists(profile) && validationProfiles[profile](elem));
    };

    var validationProfileExists = function (profile) {
        return validationProfiles[profile] && typeof validationProfiles[profile] === 'function';
    };

    return {
        check: check,
        testValidity: testValidity
    };
});
