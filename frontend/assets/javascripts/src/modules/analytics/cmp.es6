import { onConsentChange } from '@guardian/consent-management-platform';

const getConsentForVendors = (cmpVendorIds) => new Promise((resolve) => {
    if (!Array.isArray(cmpVendorIds)) {
        return resolve({});
    }

    onConsentChange(state => {
        /**
         * Loop over cmpVendorIds and pull
         * vendor specific consent from state.
         */
        resolve(cmpVendorIds.reduce((accumulator, vendorId) => {
            const consented = state.tcfv2 && state.tcfv2.vendorConsents && state.tcfv2.vendorConsents[vendorId] || false;
            return {
                ...accumulator,
                [vendorId]: consented,
            };
        }, {}));
    })
});

const checkAllTCFv2PurposesAreOptedIn = () => new Promise((resolve) => {
    onConsentChange(state => {
        resolve(state.tcfv2 && state.tcfv2.consents && Object.values(state.tcfv2.consents).every(Boolean));
    })
});

const checkCCPA = () => new Promise((resolve) => {
    onConsentChange(state => {
        resolve(state.ccpa ? !state.ccpa.doNotSell : null);
    })
});

const registerCallbackOnConsentChange = (fn) => onConsentChange(fn);

export { getConsentForVendors, checkAllTCFv2PurposesAreOptedIn, checkCCPA, registerCallbackOnConsentChange };
