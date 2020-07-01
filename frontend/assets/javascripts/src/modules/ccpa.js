// ----- Imports ----- //

// import {
//     getCookie
//   } from 'src/utils/cookie';

// ----- Functions ----- //

export const ccpaEnabled = () => {
    const useCCPA = true; // set false to switch CCPA off
    const countryId = 'US'; // getCookie('GU_country');

    return useCCPA && countryId === 'US';
};
