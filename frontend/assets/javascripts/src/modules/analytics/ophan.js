define(['src/modules/raven'],function(raven) {
    'use strict';

    function init() {
        var ophanUrl = '//j.ophan.co.uk/ophan.membership.js';
        curl('js!' + ophanUrl).then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: init
    };
});
