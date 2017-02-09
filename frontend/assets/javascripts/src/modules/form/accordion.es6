/*

This module is responsible for opening and closing different panels in the
checkout form. The user can only continue if the previous section validates.

*/

// ----- Imports ----- //

import * as payment from 'src/modules/payment';


// ----- Panels ----- //

const form = document.getElementById('payment-form');

// Benefits panel.
const benefitsExists =
	form.getElementsByClassName('js-form-panel-benefits').length > 0;
const benefitsPanel = {};

if (benefitsExists) {
	benefitsPanel.content = [ form.querySelector('.js-form-panel-benefits') ];
	benefitsPanel.editLink = form.querySelector('.js-edit-benefits');
	benefitsPanel.continue = form.querySelector('.js-continue-benefits');
}

// Name and address panel.
const nameAddressPanel = {
	content: [
		form.querySelector('.js-form-panel-name-address'),
		form.querySelector('.js-sign-in-note')
	],
	editLink: form.querySelector('.js-edit-name-address'),
	continue: form.querySelector('.js-continue-name-address')
};

// Payment panel.
const paymentPanel = {
	content: [ form.querySelector('.js-form-panel-payment') ],
	editLink: null,
	continue: null
};


// ----- Functions ----- //

// Closes a form panel.
function closePanel (panel, editable) {

	panel.content.forEach(elem => {
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
function benefitsListeners () {

	if (benefitsExists) {

		benefitsPanel.continue.addEventListener('click', () => {
			closePanel(benefitsPanel, true);
			openPanel(nameAddressPanel);
		});

		benefitsPanel.editLink.addEventListener('click', () => {
			openPanel(benefitsPanel);
			closePanel(nameAddressPanel);
			closePanel(paymentPanel);
		});

	}

}

// Sets up listeners in the name and address panel.
function nameAddressListeners () {

	nameAddressPanel.continue.addEventListener('click', () => {

		if (payment.validateForm()) {
			closePanel(nameAddressPanel, true);
			openPanel(paymentPanel);
		}

	});

	nameAddressPanel.editLink.addEventListener('click', () => {
		openPanel(nameAddressPanel);
		closePanel(paymentPanel);
	});

}


// ----- Exports ----- //

// Sets up listeners for opening and closing panels.
export function init () {

	benefitsListeners();
	nameAddressListeners();

}
