// ----- Imports ----- //

import { raven } from 'src/modules/raven';

// ----- Functions ----- //

const GEOCOUNTRY_URL = '/geocountry';
const US_COUNTRY_CODE = 'US';

let countryCode;

export const ccpaEnabled = () => {
    const useCCPA = true; // set false to switch CCPA off

    if (!useCCPA) {
        return Promise.resolve(false);
    }

    if (countryCode) {
        return Promise.resolve(countryCode === US_COUNTRY_CODE);
    }

    return fetch(GEOCOUNTRY_URL).then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('failed to get geocountry');
        }
    }).then(responseCountryCode => {
        countryCode = responseCountryCode;
        return responseCountryCode === US_COUNTRY_CODE;
    }).catch(err => {
        raven.Raven.captureException(err);
        return false;
    });
};
