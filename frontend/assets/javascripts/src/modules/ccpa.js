// ----- Imports ----- //

import {
    getCookie
  } from 'src/utils/cookie';

  // ----- Functions ----- //

  export const ccpaEnabled = () => {
    const useCCPA = true; // set false to switch CCPA off
    const countryId = getCookie('GU_country');

    return useCCPA && countryId === 'US';
  };
