import identityPopup from './identityPopup';
import identityPopupDetails from './identityPopupDetails';

export function init () {

    if (!guardian.membership.simpleHeader && !guardian.membership.guardianHeader) {
		identityPopup.init();
		identityPopupDetails.init();
	}

};
