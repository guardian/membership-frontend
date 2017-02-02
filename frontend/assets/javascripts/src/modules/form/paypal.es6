// ----- Imports ----- //

import form from 'src/modules/form/helper/formUtil';
import * as payment from 'src/modules/payment';
import 'whatwg-fetch';


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

        const checkoutForm = guardian.membership.checkoutForm;
        const amount = checkoutForm.billingPeriods[checkoutForm.billingPeriod].amount[checkoutForm.currency];
        const tier = form.elem.tier.value;

		const SETUP_PAYMENT_URL = '/paypal/setup-payment';

		fetch(SETUP_PAYMENT_URL, {
            credentials: 'include',
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({amount: amount, billingPeriod: checkoutForm.billingPeriod, currency: checkoutForm.currency, tier: tier })
		})
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
        credentials: 'include',
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
        	payment.fail({
        		type: 'PayPal',
        		code: 'PaymentError',
        		additional: 'Catch-all - unknown error in PayPal checkout.'
        	});
        },
        // We don't want to do anything here, but if this callback isn't present
        // PayPal throws a bunch of errors and then stops working.
        onCancel: () => {},

		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: makePayment

	}, '#paypal-button-checkout');

}
