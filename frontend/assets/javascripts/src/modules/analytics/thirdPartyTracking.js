
import { getCookie, setCookie } from '../../utils/cookie';

const ConsentCookieName = 'GU_TK';
const DaysToLive = 30 * 18;

const OptedIn = 'OptedIn';
const OptedOut = 'OptedOut';
const Unset = 'Unset';

const getTrackingConsent = () => {
    const cookieVal = getCookie(ConsentCookieName);
    if (cookieVal && cookieVal.split('.')[0] === '1') { return OptedIn; }
    if (cookieVal && cookieVal.split('.')[0] === '0') { return OptedOut; }
    return Unset;
};

const thirdPartyTrackingEnabled = () => getTrackingConsent() === OptedIn;

const writeTrackingConsentCookie = (trackingConsent) => {
    if (trackingConsent !== Unset) {
        const cookie = [trackingConsent === OptedIn ? '1' : '0', Date.now()].join('.');
        setCookie(ConsentCookieName, cookie, DaysToLive);
    }
};

export { getTrackingConsent, writeTrackingConsentCookie, thirdPartyTrackingEnabled, OptedIn, OptedOut, Unset, ConsentCookieName };
