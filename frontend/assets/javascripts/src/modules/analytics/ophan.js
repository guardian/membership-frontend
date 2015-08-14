/*global Raven */
define(function() {
    'use strict';

    function init() {
        var ophanUrl = '//j.ophan.co.uk/ophan.membership.js';
        require('js!' + ophanUrl).then(null, function(err) {
            Raven.captureException(err);
        });
    }

    return {
        init: init
    };
});
