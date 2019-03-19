import {
    getTrackingConsent,
    OptedIn,
    Unset,
    writeTrackingConsentCookie,
} from './analytics/thirdPartyTracking';

const BANNER = 'js-consent-banner';
const ACCEPT_BUTTON = 'js-consent-banner-accept';

function bindHandlers(elements) {
    elements.acceptButton.addEventListener('click', () => {
        writeTrackingConsentCookie(OptedIn);
        setBannerVisibility(elements);
    });

}

function setBannerVisibility(elements) {
    if (getTrackingConsent() === Unset){
        elements.banner.style.display = 'block';
    } else {
        elements.banner.style.display = 'none';
    }

}

function getElements() {
    return {
        banner: document.getElementsByClassName(BANNER)[0],
        acceptButton: document.getElementsByClassName(ACCEPT_BUTTON)[0]
    }
}

export function init() {
    const elements = getElements();
    bindHandlers(elements);
    setBannerVisibility(elements);
}



