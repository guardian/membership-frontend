// ----- Imports ----- //

import { Raven } from 'src/modules/raven';

// ----- Functions ----- //

const GEOCOUNTRY_URL = '/geocountry';
const US_COUNTRY_CODE = 'US';

let fetchCountry;

export const ccpaEnabled = () => {
    const useCCPA = true; // set false to switch CCPA off

    if (!useCCPA) {
        return Promise.resolve(false);
    }

    if (fetchCountry) {
        return fetchCountry;
    }

    fetchCountry = fetch(GEOCOUNTRY_URL).then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('failed to get geocountry');
        }
    }).then(responseCountryCode => {
        return responseCountryCode === US_COUNTRY_CODE;
    }).catch(err => {
        Raven.captureException(err);
        return false;
    });

    return fetchCountry;
};
