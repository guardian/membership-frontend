/*

This module is responsible for opening and closing different panels in the
checkout form. The user can only continue if the previous section validates.

*/

// ----- Imports ----- //

import * as payment from 'src/modules/payment';


// ----- Functions ----- //

// Looks up the benefits panel in the DOM.
function getBenefitsPanel (form) {

	return {
		content: [ form.querySelector('.js-form-panel-benefits') ],
		editLink: form.querySelector('.js-edit-benefits'),
		continue: form.querySelector('.js-continue-benefits')
	};

}

// Looks up the name/address panel in the DOM.
function getNameAddressPanel (form) {

	return {
		content: [
			form.querySelector('.js-form-panel-name-address'),
			form.querySelector('.js-sign-in-note')
		],
		editLink: form.querySelector('.js-edit-name-address'),
		continue: form.querySelector('.js-continue-name-address')
	};

}

// Looks up the payment panel in the DOM.
function getPaymentPanel (form) {

	return {
		content: [ form.querySelector('.js-form-panel-payment') ],
		editLink: null,
		continue: null
	};

}

// Retrieves references to the panels from the DOM.
function getPanels (form, benefitsExists) {

	return {
		benefits: benefitsExists ? getBenefitsPanel(form) : null,
		nameAddress: getNameAddressPanel(form),
		payment: getPaymentPanel(form)
	};

}

// Closes a form panel.
function closePanel (panel, editable) {

	panel.content.filter(elem => elem != null).forEach(elem => {
		elem.classList.add('is-hidden');
	});

	if (panel.editLink) {

		if (editable) {
			panel.editLink.classList.remove('is-hidden');
		} else {
			panel.editLink.classList.add('is-hidden');
		}

	}

}

// Opens a form panel.
function openPanel (panel) {

	panel.content.forEach(elem => {
		elem.classList.remove('is-hidden');
	});

	if (panel.editLink) {
		panel.editLink.classList.add('is-hidden');
	}

}

// Sets up listeners in the benefits panel.
function benefitsListeners (panels) {

	panels.benefits.continue.addEventListener('click', () => {
		closePanel(panels.benefits, true);
		openPanel(panels.nameAddress);
	});

	panels.benefits.editLink.addEventListener('click', () => {
		openPanel(panels.benefits);
		closePanel(panels.nameAddress);
		closePanel(panels.payment);
	});

}

// Sets up listeners in the name and address panel.
function nameAddressListeners (panels) {

	panels.nameAddress.continue.addEventListener('click', () => {

		if (payment.validateForm()) {
			closePanel(panels.nameAddress, true);
			openPanel(panels.payment);
		}

	});

	panels.nameAddress.editLink.addEventListener('click', () => {
		openPanel(panels.nameAddress);
		closePanel(panels.payment);
	});

}


// ----- Exports ----- //

// Sets up listeners for opening and closing panels.
export function init () {

	const form = document.getElementById('payment-form');

	if (form) {

		const benefitsExists =
			form.getElementsByClassName('js-form-panel-benefits').length > 0;
		const panels = getPanels(form, benefitsExists);

		if (benefitsExists) {
			benefitsListeners(panels);
		}

		nameAddressListeners(panels);

	}

}
