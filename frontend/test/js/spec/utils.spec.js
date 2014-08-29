define([
    'src/utils/router',
    'domready',
    'src/utils/user',
    'src/utils/cookie',
    '$'
], function (router, domready, userUtil, cookie, $) {

    describe('Base application utilities', function() {

        var GU_USER_COOKIE_KEY = 'GU_U';
        var GU_U_USER_COOKIE = GU_USER_COOKIE_KEY + '=WyIxMDAwMDAwMSIsImNocmlzLmZpbmNoQGd1YXJkaWFuLmNvLnVrIiwiY2hyaXNmaW5jaGRldmxvY2FsIiwiMiIsMTQwMjU2OTc2ODc5MiwxLDEzODg3NjQxNTIwMDAsdHJ1ZV0.MC0CFQCA_UeAIl3w54hpV_cdE_0RMcZ7hQIUT68uNpPYkXsAJDv2KoiM-8LvBf8;';


        beforeEach(function () {
            document.cookie += GU_U_USER_COOKIE;
        });

        afterEach(function () {
            cookie.removeCookie(GU_USER_COOKIE_KEY);
        });

        /********************************************************
         * Router - router.js
         ********************************************************/

        it('should correctly execute a matched route', function() {
            var bar = null;
            var foo = {
                setBar: function() {
                    bar = 'foo';
                }
            };

            spyOn(foo, 'setBar');

            router.match('/context.html').to(foo.setBar); // Karma default route

            domready(function () {
                router.go();
                expect(foo.setBar).toHaveBeenCalled();
            });
        });

        it('should correctly execute a wildcard route', function() {
            var bar = null;
            var foo = {
                setBar: function() {
                    bar = 'foo';
                }
            };

            spyOn(foo, 'setBar');

            router.match('*').to(foo.setBar); // Karma default route

            domready(function () {
                router.go();
                expect(foo.setBar).toHaveBeenCalled();
            });
        });

        it('should correctly execute a suffix route', function() {
            var bar = null;
            var foo = {
                setBar: function() {
                    bar = 'foo';
                }
            };

            spyOn(foo, 'setBar');

            router.match('*/context.html').to(foo.setBar); // Karma default route

            domready(function () {
                router.go();
                expect(foo.setBar).toHaveBeenCalled();
            });
        });

        /********************************************************
         * $ util - $.js, bonzo.js, qwery.js
         ********************************************************/

        it('should instantiate a bonzo.js object from a qwery.js selector using $ util', function() {

            var el = $('body');

            expect(typeof el.addClass).toEqual('function');
        });

        /********************************************************
         * Identity - user.js
         * End-to-end so includes testing of atob.js
         ********************************************************/

        xit('should read an Identity GU_U cookie correctly', function(){

            var user = userUtil.getUserFromCookie();

            expect(user.displayname).toEqual('chrisfinchdevlocal');
            expect(user.primaryemailaddress).toEqual('chris.finch@guardian.co.uk');
            expect(user.id).toEqual('10000001');
            expect(user.accountCreatedDate).toEqual(1388764152000);
        });

    });

});

