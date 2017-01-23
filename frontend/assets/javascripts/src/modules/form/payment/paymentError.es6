// ----- Imports ----- //

import $ from 'src/utils/$';
import {Raven} from 'src/modules/raven';


// ----- Setup ----- //

const genericErrorMessage = "Sorry, we weren't able to process your payment this time around. Please try again.";

const errorMessages = {
    PaymentGatewayError: {
        Fraudulent: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.',
        TransactionNotAllowed: 'Sorry we could not take your payment because your card does not support this type of purchase. Please try a different card or contact your bank to find the cause.',
        DoNotHonor: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.',
        InsufficientFunds: 'Sorry we could not take your payment because your bank indicates insufficient funds. Please try a different card or contact your bank to find the cause.',
        RevocationOfAuthorization: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.',
        GenericDecline: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.',
        UknownPaymentError: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
    }
};

const messageElement = $('.js-payment-error');


// ----- Functions ----- //

// Decides what the message text should be.
function getMessage (error) {

    const specificMessage = error && errorMessages.hasOwnProperty(error.type);

    if (specificMessage) {

        return [`${error.type}: ${error.code}`,
            errorMessages[error.type][error.code]];

    }

    return ['PaymentError', genericErrorMessage];

}


// ----- Exports ----- //

// Displays a message on the page that corresponds to the error passed.
export function showMessage (error) {

    const [sentryMessage, message] = getMessage(error);

    logError(sentryMessage)
    messageElement.text(message);
    messageElement.removeClass('is-hidden');

}

// Hides the error message.
export function hideMessage () {

    messageElement.addClass('is-hidden');

}

// Sends an error message to Sentry.
export function logError (message, level) {

    Raven.captureMessage(message, { level: level || 'error' });

}
