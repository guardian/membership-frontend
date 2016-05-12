define(['src/modules/raven'],function(raven) {
    'use strict';

    var KRUX_ID = 'JglooLwn';

    function init() {
        curl(['js!https://cdn.krxd.net/controltag?confid=' + KRUX_ID]).then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: init
    };
});
