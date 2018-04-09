
import { loadScript } from "src/utils/loadScript";
import { raven } from 'src/modules/raven';

const KRUX_ID = 'JglooLwn';

export function init() {
    loadScript('//cdn.krxd.net/controltag?confid=' + KRUX_ID, {}).then(null, function (err) {
        raven.Raven.captureException(err);
    });
}

