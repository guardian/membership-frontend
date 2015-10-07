/* global Raven */
define(['src/utils/user', 'raven'], function (user) {
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
            whitelistUrls: [ /membership\.theguardian\.com/ ],
            tags: tags,
            ignoreErrors: [ /duplicate define: jquery/ ],
            ignoreUrls: [ /platform\.twitter\.com/ ]
        }).install();
    }

    return {
        init: init
    };

});
