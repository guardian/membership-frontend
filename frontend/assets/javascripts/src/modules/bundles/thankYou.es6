import * as cookie from 'src/utils/cookie';
import { getQueryParameterByName, getPath } from 'src/utils/url';
import bean from 'bean'

'use strict';


const THANK_YOU_CTA_SELECTOR = '.bundle-thankyou__email-form__submit-button';
const THANK_YOU_CTA = document.querySelectorAll(THANK_YOU_CTA_SELECTOR);


export function init() {
    if (THANK_YOU_CTA) {
        bindSubmitButton()
        return;
    }
}

function bindSubmitButton() {
    bean.on(THANK_YOU_CTA, 'click', function (e) {
        console.log("Click here!");
    });
}
