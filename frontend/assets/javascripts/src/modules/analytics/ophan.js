/*global Raven */
define(function() {

    function init() {
        var ophanUrl = '//j.ophan.co.uk/ophan.membership.js';
        require('js!' + ophanUrl).then(null, function(err) {
            Raven.captureException(err);
            Raven.captureMessage('Ophan failed to load');
        });
    }

    return {
        init: init
    };
});
