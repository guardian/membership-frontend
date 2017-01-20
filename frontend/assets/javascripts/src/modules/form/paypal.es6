// ----- Imports ----- //

import listeners from 'src/modules/form/payment/listeners';
import * as payment from 'src/modules/payment';


// ----- Functions ----- //

// Sends request to server to setup payment, and returns Paypal token.
function setupPayment (resolve, reject) {

	if (payment.validateForm()) {

		const SETUP_PAYMENT_URL = '/paypal/setup-payment';

		fetch(SETUP_PAYMENT_URL, { method: 'POST' })
			.then(response => {
				if (response.status === 200) {
					return response.json();
				} else {
					throw 'Payment setup failed.';
				}
			})
			.then(({token}) => resolve(token))
			.catch(reject);

	} else {
		reject('Form invalid.');
	}

}

// Creates the billing agreement and retrieves the BAID as json.
function createAgreement (paypalData) {

	const CREATE_AGREEMENT_URL = '/paypal/create-agreement';

	return fetch(CREATE_AGREEMENT_URL, {
		headers: { 'Content-Type': 'application/json' },
		method: 'POST',
		body: JSON.stringify({ token: paypalData.paymentToken })
	}).then(response => {
		return response.json();
	});

}

// Creates the new member by posting the form data with the BAID.
function postForm (baid) {
	payment.postForm({ 'payment.payPalBaid': baid.token });
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
        onError: payment.fail,

		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: function (data, actions) {

			payment.showSpinner();
			createAgreement(data).then(postForm).catch(payment.fail);

	   }

	}, '#paypal-button-checkout');

	listeners.cardDisplayButtonListener();

}
