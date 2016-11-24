/**
 * Used for storing campaign codes in a cookie for readout server-side.
 * Necessary for recording campaigns in the soulmates dashboard.
 *
 */

import { setCookie } from 'src/utils/cookie';
import URLSearchParams from 'URLSearchParams';

// Checks the querystring for INTCMP (internal campaign) and records in cookie.
function recordCampaign () {

	let urlParams = new URLSearchParams(window.location.search);
	let campaignCode = urlParams.get('INTCMP');

	if (campaignCode) {
		setCookie('mem_campaign_code', campaignCode);
	}

}

export function init () {
	recordCampaign();
}
