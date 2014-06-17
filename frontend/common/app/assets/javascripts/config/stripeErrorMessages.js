define({
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
            card_not_supported: 'Sorry, this card is not accepted. Please use Visa, Mastercard or American Express'
        },
        processing_error: 'Sorry, we weren\'t able to process your payment this time around. Please try again.',
        client_validation: 'Sorry, we\'ve found some problems with your details. Please check and retype.'
    },
    generic_error: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
});