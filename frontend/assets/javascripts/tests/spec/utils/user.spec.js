define([
    'src/utils/user'
], function (userUtil) {

    describe('user utility tests', function() {

        it('idCookieAdapter test', function(){
            var dataArray = ['Doc', 'Grumpy', 'Happy', 'Sleepy', 'Bashful', 'Sneezy', 'Dopey', 'Stealthy'];
            var data = userUtil.idCookieAdapter(dataArray, 'MockString');

            expect(data.id).toEqual('Doc');
            expect(data.displayname).toEqual('Happy');
            expect(data.accountCreatedDate).toEqual('Dopey');
            expect(data.emailVerified).toEqual('Stealthy');
            expect(data.rawResponse).toEqual('MockString');
        });
    });
});
