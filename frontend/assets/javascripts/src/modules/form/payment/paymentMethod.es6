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

	paymentMethods.addClass('is-hidden');
	cardFields.removeClass('is-hidden');
	submitButton.removeClass('is-hidden');

}

// Hides the credit card fieldset, and disables the form fields.
export function hideCardFields (event) {

	event.preventDefault();

	[].forEach.call(cardFields.elements, (card) => {
		card.setAttribute('disabled', 'disabled');
	});

	paymentMethods.removeClass('is-hidden');
	cardFields.addClass('is-hidden');
	submitButton.addClass('is-hidden');

}
