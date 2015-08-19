define(['src/utils/url'], function (urlUtils) {

    describe('urlUtils', function() {

        it('Determine if a link is external', function() {
            expect(urlUtils.isExternal('https://theguardian.com/')).toEqual(true);
            expect(urlUtils.isExternal('http://theguardian.com/')).toEqual(true);
            expect(urlUtils.isExternal('http://theguardian.com')).toEqual(true);
            expect(urlUtils.isExternal('/about')).toEqual(false);
            expect(urlUtils.isExternal('/about/some/sub-page')).toEqual(false);
        });

    });

});

