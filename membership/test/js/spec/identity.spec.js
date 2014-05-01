define(['user'], function (userUtil) {
    describe('Identity module', function(){
        it('should read an Identity GU_U cookie correctly', function(){

            document.cookie += 'GU_U=WyIxMDAwMDAwMSIsImNocmlzLmZpbmNoQGd1YXJkaWFuLmNvLnVrIiwiY2hyaXNmaW5jaGRldmxvY2FsIiwiMiIsMTQwMjU2OTc2ODc5MiwxLDEzODg3NjQxNTIwMDAsdHJ1ZV0.MC0CFQCA_UeAIl3w54hpV_cdE_0RMcZ7hQIUT68uNpPYkXsAJDv2KoiM-8LvBf8;';

            var user = userUtil.getUserFromCookie();

            expect(user.displayname).toEqual('chrisfinchdevlocal');
            expect(user.primaryemailaddress).toEqual('chris.finch@guardian.co.uk');
            expect(user.id).toEqual('10000001');
            expect(user.accountCreatedDate).toEqual(138876415200000);
        });
    });
});

