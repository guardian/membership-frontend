import { Raven } from 'src/modules/raven';
import { onConsentChange } from '@guardian/consent-management-platform';

const OptedIn = 'OptedIn';
const OptedOut = 'OptedOut';

const getTrackingConsent = () => new Promise((resolve) => {
    onConsentChange(state => {
            const consentGranted = state.ccpa ? !state.ccpa.doNotSell : state.tcfv2 && Object.values(state.tcfv2.consents).every(Boolean);

            if (consentGranted) {
                resolve(OptedIn);
            } else {
                resolve(OptedOut);
            }
        });
    }).catch(err => {
        Raven.captureException(err);
        // fallback to OptedOut if there's an issue getting consent state
        return Promise.resolve(OptedOut);
    });

const thirdPartyTrackingEnabled = () => getTrackingConsent().then(consentState => consentState === OptedIn);

export { thirdPartyTrackingEnabled, OptedIn, OptedOut };
