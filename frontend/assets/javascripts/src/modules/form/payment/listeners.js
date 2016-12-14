define([
    'bean',
    'src/utils/masker',
    'src/modules/form/payment/displayCardImg',
    'src/modules/form/payment/paymentMethod'
], function (bean, masker, displayCardImg, paymentMethod) {
    'use strict';

    var CREDIT_CARD_NUMBER_ELEM = document.querySelector('.js-credit-card-number');

    /**
     * add listeners for the credit card elem for masker and display credit card image interactivity
     */
    var addPaymentListeners = function () {
        bean.on(CREDIT_CARD_NUMBER_ELEM, 'keyup blur', masker(' ', 4));

        bean.on(CREDIT_CARD_NUMBER_ELEM, 'keyup blur', function (e) {
            var input = e && e.target;
            displayCardImg(input.value);
        });
    };

    // Sets up a listener for the card payment method button.
    function cardDisplayButtonListener () {

        var cardButton = document.getElementsByClassName(
            'js-card-payment-method')[0];
        var changeLink = document.getElementsByClassName(
            'js-change-payment-method')[0];

        cardButton.addEventListener('click', paymentMethod.showCardFields);
        changeLink.addEventListener('click', paymentMethod.hideCardFields);

    }

    // Sets up the listeners defined above.
    function initListeners () {

        addPaymentListeners();
        cardDisplayButtonListener();

    }

    return {
        init: initListeners
    };
});
