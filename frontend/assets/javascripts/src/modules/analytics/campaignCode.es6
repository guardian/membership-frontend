/**
 * Used for storing campaign codes in a cookie for readout server-side.
 * Necessary for recording campaigns in the soulmates dashboard.
 *
 */

import { setCookie } from 'src/utils/cookie';
import URLSearchParams from 'URLSearchParams';

// Checks the querystring for INTCMP (internal campaign) and records in cookie.
function recordCampaign () {

	let campaignCode;

	// Don't want JS to break if there is a problem reading query params.
	// Just don't set the cookie if this happens.
	try {

		let urlParams = new URLSearchParams(window.location.search.slice(1));
		campaignCode = urlParams.get('INTCMP');

	} catch (ex) {
		console.log('Failed to retrieve campaign code:', ex.message);
	} finally {

		if (campaignCode) {
			setCookie('mem_campaign_code', campaignCode);
		}

	}

}

export function init () {
	recordCampaign();
}
