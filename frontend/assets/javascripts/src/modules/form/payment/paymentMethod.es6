// ----- Imports ----- //

import $ from 'src/utils/$'
import listeners from 'src/modules/form/payment/listeners';


// ----- Setup ----- //

let paymentMethods = $('.js-payment-methods');
let cardFields = $('.js-checkout-card-fields');
let submitButton = $('.js-submit-input');


// ----- Exports ----- //

// Unhides the credit card fieldset, and enables the form fields.
export function showCardFields (event) {

	event.preventDefault();

	[].forEach.call(cardFields.elements, (card) => {
		card.removeAttribute('disabled');
	});

	paymentMethods.hide();
	cardFields.show();
	submitButton.show();

}

// Hides the credit card fieldset, and disables the form fields.
export function hideCardFields (event) {

	event.preventDefault();

	[].forEach.call(cardFields.elements, (card) => {
		card.setAttribute('disabled', 'disabled');
	});

	paymentMethods.show();
	cardFields.hide();
	submitButton.hide();

}
