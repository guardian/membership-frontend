define([
    'src/utils/decodeBase64',
    'src/utils/user'
], function (decodeBase64, userUtil) {

    describe('decodeBase64 tests', function() {

        var GU_U_COOKIE = 'WyIxMDAwMDAyNyIsImRldmd1MzJAZmVlZG15cGl4ZWwuY29tIiwiZm1wZGV2MzIiLCIyIiwxNDE3MDA4ODMxNjg2LDAsMTQwODYwODM3MjAwMCxmYWxzZV0.MC0CFQCMS2P02pzUixTwIk6dqw-Y755E6AIUDP5E1jp8yDwb2gnRyHPyN-yDWSM;';

        it('decode GU_U cookie', function(){
            var userData = JSON.parse(decodeBase64(GU_U_COOKIE.split('.')[0]));
            var user = userUtil.idCookieAdapter(userData, GU_U_COOKIE);

            expect(user.displayname).toEqual('fmpdev32');
            expect(user.id).toEqual('10000027');
            expect(user.accountCreatedDate).toEqual(1408608372000);
        });
    });
});
