define([
    'src/utils/user',
    'src/utils/cookie',
    'ajax'
], function (userUtil, cookie, ajax) {

    describe('User Utilities', function() {

        var GU_U_USER_COOKIE = 'WyIxMDAwMDAyNyIsImRldmd1MzJAZmVlZG15cGl4ZWwuY29tIiwiZm1wZGV2MzIiLCIyIiwxNDE3MDA4ODMxNjg2LDAsMTQwODYwODM3MjAwMCxmYWxzZV0.MC0CFQCMS2P02pzUixTwIk6dqw-Y755E6AIUDP5E1jp8yDwb2gnRyHPyN-yDWSM;';
        var MEM_USER_COOKIE_KEY = 'GU_MEM';
        var GU_USER_COOKIE_KEY = 'GU_U';
        var NO_MEMBERSHIP_USER = 'no membership user';
        var errorResponse = { status: 403 };

        var friendUserDetails =  { userId: '10000027', tier: 'Friend', joinDate: 1408608382000, optIn: true };
        var nonMemberUserDetails = { userId: '10000027' };

        function setUpCookie() {
            cookie.setCookie(GU_USER_COOKIE_KEY, GU_U_USER_COOKIE, 1, true);
        }

        function removeCookie() {
            cookie.removeCookie(MEM_USER_COOKIE_KEY);
            cookie.removeCookie(GU_USER_COOKIE_KEY);
        }

        beforeEach(function () {
            removeCookie();
            setUpCookie();
        });
        afterEach(removeCookie);

        it('Read Identity GU_U cookie correctly', function(){

            var user = userUtil.getUserFromCookie();

            expect(user.displayname).toEqual('fmpdev32');
            expect(user.primaryemailaddress).toEqual('devgu32@feedmypixel.com');
            expect(user.id).toEqual('10000027');
            expect(user.accountCreatedDate).toEqual(1408608372000);
        });

        describe('GU user & Membership Tier:Friend', function() {

            beforeEach(function () {
                removeCookie();
                setUpCookie();
            });
            afterEach(removeCookie);

            it('Get Membership info from /user/me and store in GU_MEM cookie', function (done) {

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

                    done();
                }, []);
            });

            it('Get Membership info from GU_MEM cookie without hitting /user/me', function (done) {

                spyOn(ajax, 'reqwest');

                // PhantomJS double encodes cookies so this enables the getCookie inside of user.js to return the decoded value
                spyOn(cookie, 'getCookie').and.returnValue(friendUserDetails);

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

                    done();
                }, []);
            });
        });

        describe('Get Membership info from /user/me', function() {

            beforeEach(setUpCookie);
            // membership cookie not removed after each test

            it('Ajax call made and details stored in GU_MEM cookie for first call', function (done) {

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

                    done();
                });
            });

            it('Ajax call not made and subsequent calls use cookie to return details', function (done) {

                spyOn(ajax, 'reqwest');
                spyOn(cookie, 'setCookie');

                // PhantomJS double encodes cookies so this enables the getCookie inside of user.js to return the decoded value
                spyOn(cookie, 'getCookie').and.returnValue(friendUserDetails);

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(membershipUser.userId).toEqual(friendUserDetails.userId);
                    expect(membershipUser.tier).toEqual(friendUserDetails.tier);
                    expect(membershipUser.joinDate).toEqual(friendUserDetails.joinDate);
                    expect(membershipUser.optIn).toEqual(friendUserDetails.optIn);
                    expect(err).toBeUndefined();

                    expect(cookie.setCookie).not.toHaveBeenCalled();
                    expect(cookie.setCookie.calls.any()).toEqual(false);
                    expect(cookie.setCookie.calls.count()).toEqual(0);

                    done();
                });
            });
        });

        describe('GU user & Membership Tier:None', function() {

            beforeEach(function () {
                removeCookie();
                setUpCookie();
            });
            afterEach(removeCookie);

            it('Get Membership info from /user/me and store userId in GU_MEM cookie', function (done) {

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

                    done();
                }, []);
            });

            it('Get Membership info from GU_MEM cookie without hitting /user/me', function (done) {

                spyOn(ajax, 'reqwest');

                // PhantomJS double encodes cookies so this enables the getCookie inside of user.js to return the decoded value
                spyOn(cookie, 'getCookie').and.returnValue(nonMemberUserDetails);

                cookie.setCookie(MEM_USER_COOKIE_KEY, nonMemberUserDetails, 1, true);

                userUtil.getMemberDetail(function (membershipUser, err) {
                    expect(membershipUser.userId).toEqual(nonMemberUserDetails.userId);
                    expect(err).toBeUndefined();

                    expect(ajax.reqwest).not.toHaveBeenCalled();
                    expect(ajax.reqwest.calls.any()).toEqual(false);
                    expect(ajax.reqwest.calls.count()).toEqual(0);

                    done();
                }, []);
            });
        });

        describe('Not a GU user', function() {

            it('Get Membership info /user/me not called and cookie not stored, callback fired with err string "' + NO_MEMBERSHIP_USER + '"', function () {

                removeCookie();

                spyOn(ajax, 'reqwest');
                spyOn(cookie, 'setCookie');

                userUtil.getMemberDetail(function (membershipUser, err) {

                    expect(cookie.setCookie).not.toHaveBeenCalled();
                    expect(cookie.setCookie.calls.any()).toEqual(false);
                    expect(cookie.setCookie.calls.count()).toEqual(0);

                    expect(membershipUser).toBeNull();
                    expect(err).not.toBeNull();
                    expect(err.message).toEqual(NO_MEMBERSHIP_USER);

                    expect(ajax.reqwest).not.toHaveBeenCalled();
                    expect(ajax.reqwest.calls.any()).toEqual(false);
                    expect(ajax.reqwest.calls.count()).toEqual(0);
                }, []);
            });
        });
    });
});
