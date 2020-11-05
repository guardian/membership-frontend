
import { loadScript } from 'src/utils/loadScript';

const bindBuyButtonClickHandlers = () => {
    const buyButtonClassName = 'js-ticket-cta';
    const buyButtons = document.getElementsByClassName(buyButtonClassName);

    [...buyButtons].forEach(buyButton => {
        buyButton.addEventListener('click', () => {
            window.google_trackConversion({
                google_conversion_id: 618113903,
                google_conversion_label: 'iIHRCLLc--UBEO_W3qYC',
                google_custom_params: window.google_tag_params,
                google_remarketing_only: true,
            });
        })
    });
}

export function init() {
    if (window.sideLoad && window.sideLoad.paths) {
        const { remarketing } = window.sideLoad.paths;

        if (remarketing) {
            loadScript(remarketing).then(() => {
                window.google_trackConversion({
                    google_conversion_id: 971225648,
                    google_custom_params: window.google_tag_params,
                    google_remarketing_only: true,
                });

                bindBuyButtonClickHandlers();
            });
        }
    }
}

export const vendorName = 'Google Ads Conversion Tracking'
export const cmpVendorId = '5ed0eb688a76503f1016578f'
