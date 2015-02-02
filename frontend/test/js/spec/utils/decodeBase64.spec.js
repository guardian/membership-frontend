define([
    'src/utils/decodeBase64',
], function (decodeBase64) {

    describe('decodeBase64 tests', function() {

        var GU_U_COOKIE = 'WyIxMDAwMDAyNyIsImRldmd1MzJAZmVlZG15cGl4ZWwuY29tIiwiZm1wZGV2MzIiLCIyIiwxNDE3MDA4ODMxNjg2LDAsMTQwODYwODM3MjAwMCxmYWxzZV0.MC0CFQCMS2P02pzUixTwIk6dqw-Y755E6AIUDP5E1jp8yDwb2gnRyHPyN-yDWSM;';
        var GU_MEM_COOKIE = 'eyJ1c2VySWQiOiI5NDczODU2NzEiLCJyZWdOdW1iZXIiOiIyNzY0MjUiLCJmaXJzdE5hbWUiOiJ1c2VyIGZpcnN0IG5hbWUiLCJ0aWVyIjoiUGFydG5lciIsImpvaW5EYXRlIjoxNDIyNDQ3MzUxMDAwfQ';
        var GU_U_DECODED_COOKIE_STRING = '["10000027","devgu32@feedmypixel.com","fmpdev32","2",1417008831686,0,1408608372000,false]';
        var GU_MEM_DECODED_COOKIE_STRING = '{"userId":"947385671","regNumber":"276425","firstName":"user first name","tier":"Partner","joinDate":1422447351000}';
        var TEST_STRING = 'The quick brown fox jumps over the lazy dog';

        it('decode string', function(){
            var decodedString = decodeBase64(window.btoa(TEST_STRING));
            expect(typeof decodedString).toEqual('string');
            expect(decodedString).toEqual(TEST_STRING);
        });

        it('decode GU_MEM cookie', function(){
            var cookieData = decodeBase64(GU_MEM_COOKIE);
            var userData = JSON.parse(cookieData);

            expect(cookieData).toEqual(GU_MEM_DECODED_COOKIE_STRING);
            expect(typeof cookieData).toEqual('string');

            expect(userData.userId).toEqual('947385671');
            expect(userData.regNumber).toEqual('276425');
            expect(userData.firstName).toEqual('user first name');
            expect(userData.tier).toEqual('Partner');
            expect(userData.joinDate).toEqual(1422447351000);
        });

        it('decode GU_U cookie', function(){
            var cookieData = decodeBase64(GU_U_COOKIE.split('.')[0]);
            var userData = JSON.parse(cookieData);

            expect(cookieData).toEqual(GU_U_DECODED_COOKIE_STRING);
            expect(typeof cookieData).toEqual('string');

            expect(userData[0]).toEqual('10000027');
            expect(userData[1]).toEqual('devgu32@feedmypixel.com');
            expect(userData[2]).toEqual('fmpdev32');
            expect(userData[3]).toEqual('2');
            expect(userData[4]).toEqual(1417008831686);
            expect(userData[5]).toEqual(0);
            expect(userData[6]).toEqual(1408608372000);
            expect(userData[7]).toEqual(false);
        });
    });
});
