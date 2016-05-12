define(['src/utils/user','raven-js'], function (user, Raven) {
    'use strict';

    function init(dsn) {

        var tags = { build_number: guardian.membership.buildNumber };
        var cookieUser = user.getUserFromCookie();

        if (cookieUser) {
            tags.userIdentityId = cookieUser.id;
        }

        /**
         * Set up Raven, which speaks to Sentry to track errors
         */
        Raven.config(dsn, {
            whitelistUrls: [ /membership\.theguardian\.com/, /mem\.thegulocal\.com/, /localhost/ ],
            tags: tags,
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
