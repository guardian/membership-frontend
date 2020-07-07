
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
