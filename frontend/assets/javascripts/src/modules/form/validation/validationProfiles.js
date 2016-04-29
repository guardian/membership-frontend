/*global Stripe*/
define([
    'src/modules/form/validation/display'
], function (display) {
    'use strict';

    var native = function(element){
        return element.validity.valid
    };

    /**
     * is a number less than fifty (for giraffe)
     */
    var giraffe = function (element) {
        var hiddenField = document.querySelector('.js-amount-hidden');
        var amount = parseFloat(element.value);
        var hasMax = element.hasAttribute('data-max');
        var max = element.getAttribute('data-max');
        if (element.value == '') {
            amount = parseFloat(hiddenField.value);
            console.log(amount);
        }
        element.value = amount.toFixed(2);

        if (amount >= 1 && !hasMax || amount <= max) {
            return true;
        }
        return false;
    };

    /**
     * use stripe lib utility to check for a valid looking credit card number
     * @param cardElem
     * @returns {*}
     */
    var validCreditCardNumber = function (cardElem) {
        return Stripe.card.validateCardNumber(cardElem.value);
    };

    /**
     * use stripe lib utility to check for a valid looking CVC
     * @param cvcElem
     * @returns {*}
     */
    var validCVC = function (cvcElem) {
        return Stripe.card.validateCVC(cvcElem.value);
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
        var isValid = true;

        // we only want to validate expiry if the year select has a number value i.e not the default value
        if (!isNaN(parseInt(yearElem.value, 10))) {
            isValid = Stripe.card.validateExpiry(monthElem.value, yearElem.value);

            if (isValid) {
                // treat month/year inputs as a pair if month validates both month and year are valid so flush errors
                display.flushErrIds([monthElem.id, yearElem.id]);
            }
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
        var isValid = Stripe.card.validateExpiry(monthElem.value, yearElem.value);

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
        validCreditCardYear: validCreditCardYear,
        giraffe: giraffe,
        native: native
    };
});
