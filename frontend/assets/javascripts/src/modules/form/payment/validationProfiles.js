define(['stripe'], function (stripe) {
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
     * use stripe lib utility to check for a valid looking month value pre fill year with current year as the
     * select will give us a >= currentYear so here we only want to check the month is >= currentMonth
     * @param monthElem
     * @returns {*}
     */
    var validCreditCardMonth = function (monthElem) {
        var now = new Date();
        return stripe.card.validateExpiry(monthElem.value, now.getFullYear().toString());
    };

    /**
     * use stripe lib utility to check for a valid looking year value pre fill month with monthElem.value here we only
     * want to check the year is >= currentYear
     * @param yearElem
     * @returns {*}
     */
    var validCreditCardYear = function (yearElem) {
        var monthElem = document.querySelector('.js-credit-card-exp-month');
        return stripe.card.validateExpiry(monthElem.value, yearElem.value);
    };

    return {
        validCreditCardNumber: validCreditCardNumber,
        validCVC: validCVC,
        validCreditCardMonth: validCreditCardMonth,
        validCreditCardYear: validCreditCardYear
    };
});
