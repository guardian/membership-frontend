// ----- Functions ----- //

const GEOCOUNTRY_URL = '/geocountry';

export const ccpaEnabled = () => {
    const useCCPA = true; // set false to switch CCPA off

    if (!useCCPA) {
        return Promise.resolve(false);
    }

    return fetch(GEOCOUNTRY_URL).then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('failed to get country code');
        }
    }).then(data => {
        return data === 'US';
    }).catch(() => {
        return false;
    });
};
