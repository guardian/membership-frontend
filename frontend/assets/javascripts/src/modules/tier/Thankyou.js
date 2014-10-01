define([
    '$',
    'config/appCredentials',
    'src/utils/cookie',
    'src/utils/component',
    'src/utils/user'
], function ($, appCredentials, cookie, component, user) {
    'use strict';

    var self;
    var Thankyou = function () {
        self = this;
    };

    component.define(Thankyou);

    Thankyou.prototype.classes = {
        USER_EMAIL: 'js-user-email'
    };

    Thankyou.prototype.init = function (header) {
        var emailElem = this.getElem('USER_EMAIL');
        if (emailElem) {
            // TODO potentially abstract this into its own class if user details stuff grows
            // user has upgraded or joined so remove cookie then populate the user details in the header
            cookie.removeCookie(appCredentials.membership.userCookieKey);
            header.populateUserDetails();
        }
    };

    return Thankyou;
});
