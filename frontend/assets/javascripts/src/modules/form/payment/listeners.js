define([
    'bean',
    'src/utils/masker',
    'src/modules/form/payment/displayCardImg'
], function (bean, masker, displayCardImg) {

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

    return {
        addPaymentListeners: addPaymentListeners
    };
});
