import * as cookie from 'src/utils/cookie';
import { getQueryParameterByName, getPath } from 'src/utils/url';

'use strict';

const URL = window.location;
const TEST_CODES = ['price-monthly', 'price-annual'];
const CTA_LANDING_SELECTOR = '.elevated-button';
const CTA_LANDING = document.querySelectorAll(CTA_LANDING_SELECTOR);


export function init() {
    let pagePath = getPath();
    let pathRegExp = new RegExp('/uk/supporter');
    let isLandingPage = pathRegExp.test(pagePath);

    let internalCampaignCode = getQueryParameterByName("INTCMP");
    let ccRegExp = new RegExp('mem.*banner');
    let isEngamentBannerTraffic = ccRegExp.test(internalCampaignCode);

    if (isEngamentBannerTraffic || !isLandingPage) {
        return;
    }
    guardian.abPriceCTA = cookie.getCookie('ab-price-cta');

    let priceFlow = guardian.abPriceCTA|| TEST_CODES[Math.round(Math.random())];
    cookie.setCookie('ab-price-cta', priceFlow, 30, true);

    guardian.abPriceCTA = priceFlow;
    updateButtons(priceFlow);
}

function updateButtons(priceFlow){
    switch (priceFlow){
        case 'price-monthly' :

            CTA_LANDING.forEach(function(value){
                value.setAttribute('href', value.getAttribute('href')+'&pricing=monthly');
                value.querySelector('.elevated-button--pricing-monthly').style.display = 'block';
                value.querySelector('.elevated-button--pricing-annual').style.display = 'none';
            });
            break;
        case 'price-annual' :

            CTA_LANDING.forEach(function(value){
                value.setAttribute('href', value.getAttribute('href')+'&pricing=annual');
                value.querySelector('.elevated-button--pricing-monthly').style.display = 'none';
                value.querySelector('.elevated-button--pricing-annual').style.display = 'block';
            });
            break;
    }
}




