import identityPopup from './identityPopup';
import identityPopupDetails from './identityPopupDetails';

export function init () {

	let simpleHeader = document.querySelector('header.simple-header');

	if (!simpleHeader) {
		identityPopup.init();
		identityPopupDetails.init();
	}

};
