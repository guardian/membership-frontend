define(function () {

    var paymentErrMsgs = {
        invalid_request_error: {},
        api_error: {
            rate_limit: ''
        },
        card_error: {
            incorrect_number: 'Sorry, the card number that you have entered is incorrect. Please check and retype.',
            incorrect_cvc: 'Sorry, the security code that you have entered is incorrect. Please check and retype.',
            invalid_number: 'Sorry, the card number that you have entered is incorrect. Please check and retype.',
            invalid_expiry: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.',
            invalid_cvc: 'Sorry, the security code that you have entered is invalid. Please check and retype.',
            expired_card: 'Sorry, this card has expired. Please try again with another card.',
            card_declined: {
                generic_decline: 'We\'re sorry. Your card has been declined.',
                card_not_supported: 'We\'re sorry. We can\'t take payment with this type of card. Please try again using Visa, Mastercard or American Express.',
                try_again_later: 'We can\'t process your payment right now. Please try again later.'
            },
            processing_error: 'Sorry, we weren\'t able to process your payment this time around. Please try again.',
            client_validation: 'Sorry, we\'ve found some problems with your details. Please check and retype.'
        },
        generic_error: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
    };

    var getMessage = function (err) {
        var errCode = err && err.code;
        var errType = err && err.type;
        var errSection = paymentErrMsgs[errType];
        var errMsg;

        if (errSection) {
            errMsg = errSection[errCode];

            if (errCode === 'card_declined') {
                errMsg = errSection.card_declined[err.decline_code];
                if (!errMsg) {
                    errMsg = errSection.card_declined.generic_decline;
                }
            }
        }

        return errMsg || paymentErrMsgs.generic_error;
    };

    return {
        getMessage: getMessage
    };
});
