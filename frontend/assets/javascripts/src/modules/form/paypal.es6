// ----- Imports ----- //

import listeners from 'src/modules/form/payment/listeners';
import * as payment from 'src/modules/payment';


// ----- Functions ----- //

// Handles errors in the PayPal flow, reports a failed payment and throws error.
function paypalError (err) {

	payment.fail({ type: 'PayPal', code: 'PaymentError', additional: err });
	throw new Error(err);

}

// Handles the paypal setup response and retrieves the token.
function handleSetupResponse (response) {

	if (response.status === 200) {
		return response.json();
	} else {
		paypalError('PayPal payment setup request failed.');
	}

}

// Sends request to server to setup payment, and returns Paypal token.
function setupPayment (resolve, reject) {

	payment.clearErrors();

	if (payment.validateForm()) {

		const SETUP_PAYMENT_URL = '/paypal/setup-payment';

		fetch(SETUP_PAYMENT_URL, { method: 'POST' })
			.then(handleSetupResponse)
			.then(({token}) => {

				if (token) {
					resolve(token);						
				} else {
					paypalError('PayPal token came back blank.');
				}

			})
			.catch(err => {

				payment.fail({
					type: 'PayPal',
					code: 'PaymentError',
					additional: err.message
				});
				reject(err);

			});

	} else {
		reject('Form invalid.');
	}

}

// Handles failure to create agreement.
function handleAgreementFail (err) {

	payment.fail({
		type: 'PayPal',
		code: 'BAIDCreationFailed',
		additional: err
	});

}

// Creates the billing agreement and retrieves the BAID as json.
function createAgreement (paypalData) {

	const CREATE_AGREEMENT_URL = '/paypal/create-agreement';

	return fetch(CREATE_AGREEMENT_URL, {

		headers: { 'Content-Type': 'application/json' },
		method: 'POST',
		body: JSON.stringify({ token: paypalData.paymentToken })

	}).then(response => {

		if (response.status === 200) {
			return response.json();
		} else {
			paypalError('Agreement request failed.');
		}

	});

}

// Creates the new member by posting the form data with the BAID.
function postForm (baid) {
	payment.postForm({ 'payment.payPalBaid': baid });
}

// Attempts to take the payment and make the user a member.
function makePayment (data, actions) {

	payment.showSpinner();

	createAgreement(data).then(baid => {

		if (baid && baid.token) {
			postForm(baid.token);
		} else {
			paypalError('BAID came back blank.');
		}

	}).catch(err => {
		handleAgreementFail(err ? err.message : '');
	});

}


// ----- Exports ----- //

export function init () {

	paypal.Button.render({

		// Sets the environment.
		env: 'sandbox',
		// Styles the button.
		style: { color: 'blue', size: 'medium' },
		// Defines whether user sees 'continue' or 'pay now' in overlay.
		commit: true,

		// Called when user clicks Paypal button.
		payment: setupPayment,
		// Called when there is an error with the paypal payment.
		// Note that this is only reliably called on the first error, so there
		// may be some duplicate error logging when this runs alongside
		// the paypalError function above.
        onError: () => {
        	payment.fail({ type: 'PayPal', code: 'PaymentError' });
        },
        // We don't want to do anything here, but if this callback isn't present
        // PayPal throws a bunch of errors and then stops working.
        onCancel: () => {},

		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: makePayment

	}, '#paypal-button-checkout');

	listeners.cardDisplayButtonListener();

}
