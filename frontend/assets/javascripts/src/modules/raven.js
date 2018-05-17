define(['raven-js'], function (Raven) {
    'use strict';

    function init(dsn) {
        /**
         * Set up Raven, which speaks to Sentry to track errors
         */
        Raven.config(dsn, {
            whitelistUrls: [ /membership\.theguardian\.com/, /mem\.thegulocal\.com/, /localhost/ ],
            tags: { build_number: guardian.membership.buildNumber },
            ignoreErrors: [ /duplicate define: jquery/ ],
            ignoreUrls: [ /platform\.twitter\.com/ ],
            shouldSendCallback: function(data) {
                if(window.guardian.isDev) {
                    console.log('Raven', data);
                }
                return !window.guardian.isDev;
            }
        }).install();
    }

    return{
        init: init,
        Raven: Raven
    };
});
