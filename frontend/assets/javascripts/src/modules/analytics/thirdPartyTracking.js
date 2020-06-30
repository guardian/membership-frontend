
import { getCookie, setCookie } from '../../utils/cookie';
import { ccpaEnabled } from 'src/modules/ccpa';
import { onIabConsentNotification } from '@guardian/consent-management-platform';

const ConsentCookieName = 'GU_TK';
const DaysToLive = 30 * 18;

const OptedIn = 'OptedIn';
const OptedOut = 'OptedOut';
const Unset = 'Unset';

const getTrackingConsent = () => {
    if (ccpaEnabled()) {
      return new Promise((resolve) => {
        onIabConsentNotification((consentState) => {
          console.log('onIabConsentNotification --->', consentState);
          /**
           * In CCPA mode consentState will be a boolean.
           * In non-CCPA mode consentState will be an Object.
           * Check whether consentState is valid (a boolean).
           * */
          if (typeof consentState !== 'boolean') {
            throw new Error('consentState not a boolean');
          } else {
            // consentState true means the user has OptedOut
            resolve(consentState ? OptedOut : OptedIn);
          }
        });
      }).catch(() => {
        // fallback to OptedOut if there's an issue getting consentState
        return Promise.resolve(OptedOut);
      });
    }

    const cookieVal = getCookie(ConsentCookieName);

    console.log('cookieVal --->', cookieVal)

    if (cookieVal) {
      const consentVal = cookieVal.split('.')[0];

      if (consentVal === '1') {
        return Promise.resolve(OptedIn);
      } else if (consentVal === '0') {
        return Promise.resolve(OptedOut);
      }
    }

    return Promise.resolve(Unset);
  };

const thirdPartyTrackingEnabled = () => getTrackingConsent().then(consentState => consentState === OptedIn);

const writeTrackingConsentCookie = (trackingConsent) => {
    if (trackingConsent !== Unset) {
        const cookie = [trackingConsent === OptedIn ? '1' : '0', Date.now()].join('.');
        setCookie(ConsentCookieName, cookie, DaysToLive);
    }
};

export { getTrackingConsent, writeTrackingConsentCookie, thirdPartyTrackingEnabled, OptedIn, OptedOut, Unset, ConsentCookieName };
