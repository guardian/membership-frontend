define([
    '$',
    'src/modules/userDetails',
    'ajax',
    'src/utils/cookie',
    'src/utils/user'
], function ($, userDetails, ajax, cookie, userUtil) {

    var GU_USER_COOKIE_KEY = 'GU_U',
        GU_U_USER_COOKIE = 'WyIxMjM0NTQzMjIiLCJtYXJjLmhpYmJpbnNAZ3VhcmRpYW4uY28udWsiLCJtYXJjaGliYmlucyIsIjIiLDE0MjA2NDEwMDg2NjcsMCwxNDExMDU1MzU5MDAwLGZhbHNlXQ.MC0CFQCeT6NHbQyTpMoiEXQC3rN1djN-sAIUFMI5ASoIQ06tY1e1zjKWNcg5Hlw',

        memberDetails = {
            userId: '123454322',
            firstName: 'Marc',
            tier: 'Patron',
            joinDate: 1411057402000
        },

        nonMemberDetails = {
            userId: '123454322'
        },

        addCookies = function (member) {
            cookie.setCookie(GU_USER_COOKIE_KEY, GU_U_USER_COOKIE, 1, true);
            var details = member ? memberDetails : nonMemberDetails;
            spyOn(cookie, 'getDecodedCookie').and.returnValue(details);
        },

        removeCookies = function () {
            cookie.removeCookie(GU_USER_COOKIE_KEY);
        };

    describe('User state classes', function () {

        var addClasses = function (member) {
                // This is hardcoded in main.scala.html
                $(document.documentElement).addClass(userDetails._.CLASS_NAMES.signedOut);
            },

            removeClasses = function () {
                $(document.documentElement).removeClass(userDetails._.CLASS_NAMES.signedIn);
                $(document.documentElement).removeClass(userDetails._.CLASS_NAMES.hasTier);
            };

        beforeEach(function () {
            addClasses();
        });

        afterEach(function () {
            removeClasses();
            removeCookies();
        });

        it('Should not add any classes for anonymous users', function () {
            userDetails.init();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.signedIn)).toBeFalsy();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.hasTier)).toBeFalsy();
        });

        it('Should add signed in class for authenticated users', function () {
            addCookies(true);
            userDetails.init();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.signedIn)).toBeTruthy();
        });

        it('Should add member tier class for authenticated members', function () {
            addCookies(true);
            userDetails.init();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.signedIn)).toBeTruthy();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.hasTier)).toBeTruthy();
        });

        it('Should not add member tier class for authenticated non-members', function () {
            addCookies(false);
            userDetails.init();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.signedIn)).toBeTruthy();
            expect($(document.documentElement).hasClass(userDetails._.CLASS_NAMES.hasTier)).toBeFalsy();
        });
    });

    describe('Populate user details', function () {

        var fixtureId = 'userDetails',
            fixture = '<ul>' +
                        '<li class="js-user-displayname"></li>' +
                        '<li class="js-user-firstName"></li>' +
                        '<li class="js-user-tier"></li>' +
                      '</ul>';

        var addFixtures = function (member) {
                $(document.body).append('<div id="' + fixtureId + '">' + fixture + '</div>');
            },

            removeFixtures = function () {
                $('#' + fixtureId).remove();
            };

        beforeEach(function () {
            addFixtures();
        });

        afterEach(function () {
            removeFixtures();
            removeCookies();
        });

        it('Should not attempt to inject user details for anonymous users', function () {
            userDetails.init();
            expect($('.js-user-displayname').text()).toEqual('');
            expect($('.js-user-firstName').text()).toEqual('');
            expect($('.js-user-tier').text()).toEqual('');
        });

        it('Should inject user details for authenticated users', function () {
            addCookies(false);
            userDetails.init();
            expect($('.js-user-displayname').text()).toEqual('marchibbins');
            expect($('.js-user-firstName').text()).toEqual('');
            expect($('.js-user-tier').text()).toEqual('');
        });

        it('Should inject user details for authenticated members', function () {
            addCookies(true);
            userDetails.init();
            expect($('.js-user-displayname').text()).toEqual('marchibbins');
            expect($('.js-user-firstName').text()).toEqual(memberDetails.firstName);
            expect($('.js-user-tier').text()).toEqual(memberDetails.tier);
        });
    });
});
