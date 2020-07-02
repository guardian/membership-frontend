import {
    getTrackingConsent,
    OptedIn,
    Unset,
    writeTrackingConsentCookie,
} from './analytics/thirdPartyTracking';
import { getCookie } from '../utils/cookie';

const BANNER = 'js-consent-banner';
const ACCEPT_BUTTON = 'js-consent-banner-accept';

function bindHandlers(elements) {
    elements.acceptButton.addEventListener('click', () => {
        writeTrackingConsentCookie(OptedIn);
        setBannerVisibility(elements);
    });

}

function hideBanner(elements) {
    elements.banner.style.display = 'none';
}

function showBanner(elements) {
    elements.banner.style.display = 'block';
}

function setBannerVisibility(elements) {
    getTrackingConsent().then((consentState) => {
        const visible = getCookie('_post_deploy_user') !== 'true' && consentState === Unset;

        if (visible){
            showBanner(elements);
        } else {
            hideBanner(elements);
        }
    });
}

function getElements() {
    return {
        banner: document.getElementsByClassName(BANNER)[0],
        acceptButton: document.getElementsByClassName(ACCEPT_BUTTON)[0]
    }
}

export function init(ccpaEnabled) {
    const elements = getElements();

    if (ccpaEnabled) {
        hideBanner(elements);
    } else {
        bindHandlers(elements);
        setBannerVisibility(elements);
    }
}



