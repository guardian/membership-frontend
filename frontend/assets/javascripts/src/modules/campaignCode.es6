/**
 * Used for storing campaign codes in a cookie for readout server-side.
 * Necessary for recording campaigns in the soulmates dashboard.
 *
 */

import { setCookie } from 'src/utils/cookie';

// Checks the querystring for INTCMP (internal campaign) and records in cookie.
function recordCampaign () {

	let urlParams = new URLSearchParams(window.location.search);
	let campaignCode = urlParams.get('INTCMP');

	if (campaignCode) {
		setCookie('mem_campaigncode', campaignCode);
	}

}

export function init () {
	recordCampaign();
}
