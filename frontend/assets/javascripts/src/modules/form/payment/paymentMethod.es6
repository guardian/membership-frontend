let paymentMethods = document.getElementsByClassName('js-payment-methods')[0];
let cardFields = document.getElementsByClassName('js-checkout-card-fields')[0];
let submitButton = document.getElementsByClassName('js-submit-input')[0];

// Unhides the credit card fieldset, and enables the form fields.
export function showCardFields (event) {

	event.preventDefault();

	[].forEach.call(cardFields.elements, (card) => {
		card.removeAttribute('disabled');
	});

	paymentMethods.classList.add('is-hidden');
	cardFields.classList.remove('is-hidden');
	submitButton.classList.remove('is-hidden');

}

// Hides the credit card fieldset, and disables the form fields.
export function hideCardFields (event) {

	event.preventDefault();

	[].forEach.call(cardFields.elements, (card) => {
		card.setAttribute('disabled', 'disabled');
	});

	paymentMethods.classList.remove('is-hidden');
	cardFields.classList.add('is-hidden');
	submitButton.classList.add('is-hidden');

}
