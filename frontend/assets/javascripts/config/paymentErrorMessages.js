define(function () {
    'use strict';

    var CREDIT_CARD_NUMBER_ELEM = document.querySelector('.js-credit-card-number');
    var CREDIT_CARD_CVC_ELEM = document.querySelector('.js-credit-card-cvc');
    var CREDIT_CARD_MONTH_ELEM = document.querySelector('.js-credit-card-exp-month');
    var CREDIT_CARD_YEAR_ELEM = document.querySelector('.js-credit-card-exp-year');

    var paymentErrMsgs = {
        invalid_request_error: {},
        api_error: {
            elem: CREDIT_CARD_NUMBER_ELEM,
            rate_limit: ''
        },
        card_error: {
            incorrect_number: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                msg: 'Sorry, the card number that you have entered is incorrect. Please check and retype.'
            },
            incorrect_cvc: {
                elem: CREDIT_CARD_CVC_ELEM,
                msg: 'Sorry, the security code that you have entered is incorrect. Please check and retype.'
            },
            invalid_number: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                msg: 'Sorry, the card number that you have entered is incorrect. Please check and retype.'
            },
            invalid_expiry: {
                elem: CREDIT_CARD_MONTH_ELEM,
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_expiry_month: {
                elem: CREDIT_CARD_MONTH_ELEM,
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_expiry_year: {
                elem: CREDIT_CARD_YEAR_ELEM,
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_cvc: {
                elem: CREDIT_CARD_CVC_ELEM,
                msg: 'Sorry, the security code that you have entered is invalid. Please check and retype.'
            },
            expired_card: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                msg: 'Sorry, this card has expired. Please try again with another card.'
            },
            card_declined: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                generic_decline: 'We\'re sorry. Your card has been declined.',
                card_not_supported: 'We\'re sorry. We can\'t take payment with this type of card. Please try again using Visa, Mastercard or American Express.',
                try_again_later: 'We can\'t process your payment right now. Please try again later.'
            },
            processing_error: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                msg: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
            },
            client_validation: {
                elem: CREDIT_CARD_NUMBER_ELEM,
                msg: 'Sorry, we\'ve found some problems with your details. Please check and retype.'
            }
        },
        generic_error: {
            elem: CREDIT_CARD_NUMBER_ELEM,
            msg: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
        }
    };

    var getMessage = function (err) {
        var errCode = err && err.code;
        var errType = err && err.type;
        var errSection = paymentErrMsgs[errType];
        var errMsg;

        if (errSection) {
            errMsg = errSection[errCode].msg;

            if (errCode === 'card_declined') {
                errMsg = errSection.card_declined[err.decline_code];
                if (!errMsg) {
                    errMsg = errSection.card_declined.generic_decline;
                }
            }
        }

        return errMsg || paymentErrMsgs.generic_error.msg;
    };

    var getElement = function(err) {
        var errCode = err && err.code;
        var errType = err && err.type;
        var errSection = paymentErrMsgs[errType];

        if (errSection) {
            return errSection.elem || errSection[errCode].elem;
        }
        return CREDIT_CARD_NUMBER_ELEM
    };

    return {
        getMessage: getMessage,
        getElement: getElement
    };
});
