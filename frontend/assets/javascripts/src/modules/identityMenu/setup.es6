import identityPopup from './identityPopup';
import identityPopupDetails from './identityPopupDetails';

export function init () {

	if (!guardian.membership.simpleHeader) {
		identityPopup.init();
		identityPopupDetails.init();
	}

};
