import * as cookie from 'src/utils/cookie';
import {getQueryParameterByName, getPath} from 'src/utils/url';


export function init() {
    //The PayPal AB test variant is initially passed in as a query string param
    //but this is not passed through to the thank you page, so we also store it
    //in a cookie to enable GA tracking of the variant from the thank you page.
    const paramName = "paypalTest";
    guardian.payPalTestVariant = getQueryParameterByName(paramName);
    if (guardian.payPalTestVariant) {
        cookie.setCookie(paramName, guardian.payPalTestVariant, 3);
    } else {
        guardian.payPalTestVariant = cookie.getCookie(paramName);
    }
}
