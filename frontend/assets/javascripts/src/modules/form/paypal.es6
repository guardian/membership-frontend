
import listeners from 'src/modules/form/payment/listeners';
import * as payment from 'src/modules/payment';


// Sends request to server to setup payment, and returns Paypal token.
function setupPayment (resolve, reject) {
    payment.open()
	if (payment.validateForm()) {

		const SETUP_PAYMENT_URL = '/paypal/setup-payment';

		paypal.request.post(SETUP_PAYMENT_URL)
			.then(data => {
				resolve(data.token);
			})
			.catch(err => {
				reject(err);
			});

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

export function init () {
    payment.hello();
	paypal.Button.render({

		// Sets the environment.
		env: 'sandbox',
		// Styles the button.
		style: { color: 'blue', size: 'medium' },
		// Defines whether user sees 'continue' or 'pay now' in overlay.
		commit: true,

		// Called when user clicks Paypal button.
		payment: setupPayment,
        //Oncancel
        onCancel: payment.close,
        onError: payment.failure,
		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: function (data, actions) {

			createAgreement(data).then(postForm).catch(err => {
				alert(err);
			});

	   }

	}, '#paypal-button-checkout');

	listeners.cardDisplayButtonListener();

}
