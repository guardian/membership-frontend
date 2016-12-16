import ajax from 'ajax';
import form from 'src/modules/form/helper/formUtil';
import validity from 'src/modules/form/validation/validity';
import serializer from 'src/modules/form/helper/serializer';
import utilsHelper from 'src/utils/helper';
import listeners from 'src/modules/form/payment/listeners';

// Validates the form; returns true if the form is valid, false otherwise.
function formValid () {

	form.elems.map(function (elem) {
		validity.check(elem);
	});

	let formHasErrors = form.errs.length > 0;
	return !formHasErrors;

}

// Sends request to server to setup payment, and returns Paypal token.
function setupPayment (resolve, reject) {

	if (formValid()) {

		const SETUP_PAYMENT_URL = '/paypal-setup-payment';

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

	const CREATE_AGREEMENT_URL = '/paypal-create-agreement';

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

	data = serializer(utilsHelper.toArray(form.elem.elements),
		{ 'payment.payPalBaid': baid.token });
	
	ajax({
		url: form.elem.action,
		method: 'post',
		data: data,
		success: function (successData) {
			window.location.assign(successData.redirect);
		},
		error: function (errData) {
			alert(err);
		}
	});

}

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

		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: function (data, actions) {

			createAgreement(data).then(postForm).catch(err => {
				alert(err);
			});

	   }
			
	}, '#paypal-button-checkout');

	listeners.cardDisplayButtonListener();

}
