define(['bean'], function (bean) {
    'use strict';

    var PAYMENT_OPTIONS_CONTAINER_ELEM = document.querySelector('.js-payment-options-container');
    var FORM_SUBMIT_PRICE_OPTION_ELEM = document.querySelector('.js-submit-price-option');
    var CARD_DETAILS_NOTE_ELEM = document.querySelector('.js-card-details-note');
    var CARD_NOTE_CHARGE_ELEM = CARD_DETAILS_NOTE_ELEM.querySelector('.js-card-note-pricing-charge');
    var CARD_NOTE_PERIOD_ELEM = CARD_DETAILS_NOTE_ELEM.querySelector('.js-card-note-pricing-period');
    var CARD_NOTE_PAYMENT_TAKEN_ELEM = CARD_DETAILS_NOTE_ELEM.querySelector('.js-card-note-payment-taken');

    var init = function () {
        if (PAYMENT_OPTIONS_CONTAINER_ELEM) {
            addListeners();
            populateCardNote(PAYMENT_OPTIONS_CONTAINER_ELEM.querySelector('[checked]').value);
        }
    };

    /**
     * add listeners for payment options
     */
    var addListeners = function () {
        bean.on(PAYMENT_OPTIONS_CONTAINER_ELEM, 'click', 'input', function (e) {
            var input = e && e.target;
            var paymentOptionPrice = input.getAttribute('data-pricing-option-amount');

            populateCardNote(input.value);
            FORM_SUBMIT_PRICE_OPTION_ELEM.textContent = paymentOptionPrice;
        });
    };

    /**
     * display different information dependant on chosen payment option
     * @param period
     */
    var populateCardNote = function(period) {
        var attr = 'data-' + period;

        CARD_NOTE_CHARGE_ELEM.textContent = CARD_NOTE_CHARGE_ELEM.getAttribute(attr);
        CARD_NOTE_PERIOD_ELEM.textContent = CARD_NOTE_PERIOD_ELEM.getAttribute(attr);
        CARD_NOTE_PAYMENT_TAKEN_ELEM.innerHTML = CARD_NOTE_PAYMENT_TAKEN_ELEM.getAttribute(attr);
    };

    return {
        init: init
    };
});
