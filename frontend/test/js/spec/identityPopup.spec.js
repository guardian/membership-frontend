define(['src/modules/identityPopup'], function (identityPopup) {

    var HREF = 'https://profile.thegulocal.com/signin?returnUrl=https://mem.thegulocal.com&skipConfirmation=true';

    describe('Identity Popup Tests', function () {

        it('returnUrl populated with currentUrl', function () {
            var currentUrl = '/join';

            expect(identityPopup.populateReturnUrl(HREF, currentUrl)).toBe(
                'https://profile.thegulocal.com/signin?returnUrl=https://mem.thegulocal.com' + currentUrl + '&skipConfirmation=true'
            );
        });

        it('returnUrl populated with currentUrl that contains parameters', function () {
            var currentUrl = '/join?param1=1&param2=2';

            expect(identityPopup.populateReturnUrl(HREF, currentUrl)).toBe(
                'https://profile.thegulocal.com/signin?returnUrl=https://mem.thegulocal.com' + currentUrl + '&skipConfirmation=true'
            );
        });
    });

});
