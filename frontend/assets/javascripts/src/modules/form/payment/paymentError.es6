// ----- Setup ----- //

const errorMessages = {
    PaymentGatewayError: {
        Fraudulent: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
        TransactionNotAllowed: 'Sorry we could not take your payment because your card does not support this type of purchase. Please try a different card or contact your bank to find the cause.'
        DoNotHonor: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
        InsufficientFunds: 'Sorry we could not take your payment because your bank indicates insufficient funds. Please try a different card or contact your bank to find the cause.'
        RevocationOfAuthorization: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
        GenericDecline: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
        UknownPaymentError: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
    }
};
