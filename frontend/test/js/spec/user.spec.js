define([
    'src/utils/user',
    'src/utils/cookie',
    'ajax'
], function (userUtil, cookie, ajax) {

    describe('User Utilities', function() {

        var GU_U_USER_COOKIE = 'WyIxMDAwMDAyNyIsImRldmd1MzJAZmVlZG15cGl4ZWwuY29tIiwiZm1wZGV2MzIiLCIyIiwxNDE3MDA4ODMxNjg2LDAsMTQwODYwODM3MjAwMCxmYWxzZV0.MC0CFQCMS2P02pzUixTwIk6dqw-Y755E6AIUDP5E1jp8yDwb2gnRyHPyN-yDWSM;';
        var MEM_USER_COOKIE_KEY = 'GU_MEM';
        var GU_USER_COOKIE_KEY = 'GU_U';
        var errorResponse = { message: 403 };

        var friendUserDetails =  { userId: '10000027', tier: 'Friend', joinDate: 1408608382000, optIn: true };
        var nonMemberUserDetails = { userId: '10000027' };

        function setUpCookie() {
            // clear potential initial cookies
            removeCookie();
            cookie.setCookie(GU_USER_COOKIE_KEY, GU_U_USER_COOKIE, 1, true);
        }

        function removeCookie() {
            cookie.removeCookie(MEM_USER_COOKIE_KEY);
            cookie.removeCookie(GU_USER_COOKIE_KEY);
        }

        beforeEach(setUpCookie);
        afterEach(removeCookie);

        it('Read Identity GU_U cookie correctly', function(){

            var user = userUtil.getUserFromCookie();

            expect(user.displayname).toEqual('fmpdev32');
            expect(user.primaryemailaddress).toEqual('devgu32@feedmypixel.com');
            expect(user.id).toEqual('10000027');
            expect(user.accountCreatedDate).toEqual(1408608372000);
        });

        describe('GU user & Membership Tier:Friend', function() {

            beforeEach(setUpCookie);
            afterEach(removeCookie);

            it('Get Membership info from /user/me and store in GU_MEM cookie', function () {

                spyOn(ajax, 'reqwest').and.callFake(function (ajaxParams) {
                    ajaxParams.success(friendUserDetails);
                });

                spyOn(cookie, 'setCookie');

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(membershipUser.userId).toEqual(friendUserDetails.userId);
                    expect(membershipUser.tier).toEqual(friendUserDetails.tier);
                    expect(membershipUser.joinDate).toEqual(friendUserDetails.joinDate);
                    expect(membershipUser.optIn).toEqual(friendUserDetails.optIn);
                    expect(err).toBeUndefined();

                    expect(cookie.setCookie).toHaveBeenCalled();
                    expect(cookie.setCookie.calls.any()).toEqual(true);
                    expect(cookie.setCookie.calls.count()).toEqual(1);
                    expect(cookie.setCookie.calls.argsFor(0)).toEqual([MEM_USER_COOKIE_KEY, friendUserDetails]);
                });
            });

            it('Get Membership info from GU_MEM cookie without hitting /user/me', function () {

                spyOn(ajax, 'reqwest');

                cookie.setCookie(MEM_USER_COOKIE_KEY, friendUserDetails, 1, true);

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(membershipUser.userId).toEqual(friendUserDetails.userId);
                    expect(membershipUser.tier).toEqual(friendUserDetails.tier);
                    expect(membershipUser.joinDate).toEqual(friendUserDetails.joinDate);
                    expect(membershipUser.optIn).toEqual(friendUserDetails.optIn);
                    expect(err).toBeUndefined();

                    expect(ajax.reqwest).not.toHaveBeenCalled();
                    expect(ajax.reqwest.calls.any()).toEqual(false);
                    expect(ajax.reqwest.calls.count()).toEqual(0);
                });
            });
        });

        describe('GU user & Membership Tier:None', function() {

            beforeEach(setUpCookie);
            afterEach(removeCookie);

            it('Get Membership info from /user/me and store userId in GU_MEM cookie', function () {

                spyOn(ajax, 'reqwest').and.callFake(function (ajaxParams) {
                    //mimic 403 from /user/me
                    ajaxParams.error(errorResponse);
                });

                spyOn(cookie, 'setCookie');

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(err).not.toBeUndefined();
                    expect(err.message).toEqual(errorResponse.message);
                    expect(membershipUser).toBeNull();

                    expect(cookie.setCookie).toHaveBeenCalled();
                    expect(cookie.setCookie.calls.any()).toEqual(true);
                    expect(cookie.setCookie.calls.count()).toEqual(1);
                    expect(cookie.setCookie.calls.argsFor(0)).toEqual([MEM_USER_COOKIE_KEY, nonMemberUserDetails]);
                });
            });

            it('Get Membership info from GU_MEM cookie without hitting /user/me', function () {

                spyOn(ajax, 'reqwest');

                cookie.setCookie(MEM_USER_COOKIE_KEY, nonMemberUserDetails, 1, true);

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(membershipUser.userId).toEqual(nonMemberUserDetails.userId);
                    expect(err).toBeUndefined();

                    expect(ajax.reqwest).not.toHaveBeenCalled();
                    expect(ajax.reqwest.calls.any()).toEqual(false);
                    expect(ajax.reqwest.calls.count()).toEqual(0);
                });
            });
        });

        describe('Not a GU user', function() {

            it('Get Membership info /user/me not called and cookie not stored', function () {

                var callbackMethod = {
                    notCalled: function () { /*not called*/ }
                };

                var callback = function (membershipUser, err) {
                    callbackMethod.notCalled(membershipUser, err);
                };

                removeCookie();

                spyOn(ajax, 'reqwest');
                spyOn(cookie, 'setCookie');
                spyOn(callbackMethod, 'notCalled')

                userUtil.getMemberDetail(callback);

                expect(cookie.setCookie).not.toHaveBeenCalled();
                expect(cookie.setCookie.calls.any()).toEqual(false);
                expect(cookie.setCookie.calls.count()).toEqual(0);

                expect(callbackMethod.notCalled).not.toHaveBeenCalled();
                expect(callbackMethod.notCalled.calls.any()).toEqual(false);
                expect(callbackMethod.notCalled.calls.count()).toEqual(0);

                expect(ajax.reqwest).not.toHaveBeenCalled();
                expect(ajax.reqwest.calls.any()).toEqual(false);
                expect(ajax.reqwest.calls.count()).toEqual(0);

            });
        });
    });
});
