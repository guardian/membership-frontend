let cardFields = document.getElementByClassname('js-checkout-card-fields');

// Unhides the credit card fieldset, and enables the form fields.
export function showCardFields () {

	cardFields.elements.forEach(card => {
		card.removeAttribute('disabled');
	});

	cardFields.classList.remove('is-hidden');

}

// Hides the credit card fieldset, and disables the form fields.
export function hideCardFields () {

	cardFields.elements.forEach(card => {
		card.setAttribute('disabled', 'disabled');
	});

	cardFields.classList.add('is-hidden');

}
