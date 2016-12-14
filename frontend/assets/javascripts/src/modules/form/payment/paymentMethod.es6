let cardFields = document.getElementsByClassName('js-checkout-card-fields')[0];
let submitButton = document.getElementsByClassName('js-submit-input')[0];

// Unhides the credit card fieldset, and enables the form fields.
function showCardFields () {

	[].forEach.call(cardFields.elements, (card) => {
		card.removeAttribute('disabled');
	});

	cardFields.classList.remove('is-hidden');
	submitButton.classList.remove('is-hidden');

}

// Hides the credit card fieldset, and disables the form fields.
function hideCardFields () {

	[].forEach.call(cardFields.elements, (card) => {
		card.setAttribute('disabled', 'disabled');
	});

	cardFields.classList.add('is-hidden');
	submitButton.classList.add('is-hidden');

}

// Toggles hiding or showing the card fields.
export function toggleCardFields () {

	if (cardFields.classList.contains('is-hidden')) {
		showCardFields();
	} else {
		hideCardFields();
	}

}
