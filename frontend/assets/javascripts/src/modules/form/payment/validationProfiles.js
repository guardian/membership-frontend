define([
    'stripe',
    'src/modules/form/validation/display'
], function (stripe, display) {
    'use strict';

    /**
     * use stripe lib utility to check for a valid looking credit card number
     * @param cardElem
     * @returns {*}
     */
    var validCreditCardNumber = function (cardElem) {
        return stripe.card.validateCardNumber(cardElem.value);
    };

    /**
     * use stripe lib utility to check for a valid looking CVC
     * @param cvcElem
     * @returns {*}
     */
    var validCVC = function (cvcElem) {
        return stripe.card.validateCVC(cvcElem.value);
    };

    /**
     * use stripe lib utility to check for a valid looking date.
     * If the date is valid then flush errors for both the month and year when month input is validated as we need
     * to treat month/year inputs as a pair for validation
     * @param monthElem
     * @returns {*}
     */
    var validCreditCardMonth = function (monthElem) {
        var yearElem = document.querySelector('.js-credit-card-exp-year');
        var isValid = stripe.card.validateExpiry(monthElem.value, yearElem.value);

        if (isValid) {
            // treat month/year inputs as a pair if month validates both month and year are valid so flush errors
            display.flushErrIds([monthElem.id, yearElem.id]);
        }

        return isValid;
    };

    /**
     * use stripe lib utility to check for a valid looking date.
     * If the date is valid then flush errors for both the month and year when year input is validated as we need
     * to treat month/year inputs as a pair for validation
     * @param yearElem
     * @returns {*}
     */
    var validCreditCardYear = function (yearElem) {
        var monthElem = document.querySelector('.js-credit-card-exp-month');
        var isValid = stripe.card.validateExpiry(monthElem.value, yearElem.value);

        if (isValid) {
            // treat month/year inputs as a pair if year validates both year and month are valid so flush errors
            display.flushErrIds([monthElem.id, yearElem.id]);
        }

        return isValid;
    };

    return {
        validCreditCardNumber: validCreditCardNumber,
        validCVC: validCVC,
        validCreditCardMonth: validCreditCardMonth,
        validCreditCardYear: validCreditCardYear
    };
});
