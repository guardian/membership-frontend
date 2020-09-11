
import { loadScript } from 'src/utils/loadScript';

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
            });
        }
    }
}

export const vendorName = 'Google Ads Conversion Tracking'
export const cmpVendorId = '5ed0eb688a76503f1016578f'
