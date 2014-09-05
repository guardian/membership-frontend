define([
    '$',
    'src/utils/component',
    'src/utils/user'
], function ($, component, user) {
    'use strict';

    var self;
    var Thankyou = function () {
        self = this;
    };

    component.define(Thankyou);

    Thankyou.prototype.classes = {
        USER_EMAIL: 'js-user-email'
    };

    Thankyou.prototype.populateUserDetails = function () {

        if (this.user) {
            $(this.getElem('USER_EMAIL')).text(this.user.primaryemailaddress);
        }
    };

    Thankyou.prototype.init = function () {
        this.user = user.getUserFromCookie();
        this.populateUserDetails();
    };

    return Thankyou;
});
