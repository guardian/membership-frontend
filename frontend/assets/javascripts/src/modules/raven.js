/* global Raven */
define(['raven'], function () {
    'use strict';

    function init(dsn) {
        /**
         * Set up Raven, which speaks to Sentry to track errors
         */
        Raven.config(dsn, {
            whitelistUrls: ['membership.theguardian.com/assets/'],
            tags: { build_number: guardian.membership.buildNumber },
            ignoreErrors: [
                /duplicate define: jquery/
            ]
        }).install();
    }

    return {
        init: init
    };

});
