
import { loadScript } from 'src/utils/loadScript';

export function init() {
    if (window.sideLoad && window.sideLoad.paths) {
        const { remarketing } = window.sideLoad.paths;

        if (remarketing) {
            window.google_conversion_id = 971225648,
            window.google_custom_params = window.google_tag_params,
            window.google_remarketing_only = true;

            loadScript(remarketing);
        }
    }
}
