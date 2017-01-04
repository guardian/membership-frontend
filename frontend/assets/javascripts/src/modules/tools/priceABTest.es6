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
        updateButtons(TEST_CODES[0], false);
        return;
    }
    guardian.abPriceCTA = cookie.getCookie('ab-price-cta');

    let priceFlow = guardian.abPriceCTA|| TEST_CODES[Math.round(Math.random())];
    cookie.setCookie('ab-price-cta', priceFlow, 30, true);

    guardian.abPriceCTA = priceFlow;
    updateButtons(priceFlow, true);
}

function updateButtons(priceFlow, modifyNextstep){
    switch (priceFlow){
        case 'price-monthly' :
            for(var i = 0, len = CTA_LANDING.length ; i < len ; i++) {
                var element = CTA_LANDING[i];
                if(modifyNextstep) {
                    element.setAttribute('href', element.getAttribute('href')+'&pricing=monthly');
                }
                element.querySelector('.elevated-button--pricing-placeholder').style.display = 'none';
                element.querySelector('.elevated-button--pricing-monthly').style.display = 'block';
                element.querySelector('.elevated-button--pricing-annual').style.display = 'none';
            }
            break;

        case 'price-annual' :

            for(var i = 0, len = CTA_LANDING.length ; i < len ; i++) {
                var element = CTA_LANDING[i];

                if(modifyNextstep) {
                    element.setAttribute('href', element.getAttribute('href') + '&pricing=annual');
                }
                element.querySelector('.elevated-button--pricing-placeholder').style.display = 'none';
                element.querySelector('.elevated-button--pricing-monthly').style.display = 'none';
                element.querySelector('.elevated-button--pricing-annual').style.display = 'block';
            }
            break;
    }
}
