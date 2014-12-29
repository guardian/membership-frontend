define(['src/utils/analytics/ga'], function (googleAnalytics) {

    describe('Google Analytics utilities', function() {

        it('Determine if a link is external', function() {
            expect(googleAnalytics.isExternalLink('https://theguardian.com/')).toEqual(true);
            expect(googleAnalytics.isExternalLink('http://theguardian.com/')).toEqual(true);
            expect(googleAnalytics.isExternalLink('http://theguardian.com')).toEqual(true);
            expect(googleAnalytics.isExternalLink('/about')).toEqual(false);
            expect(googleAnalytics.isExternalLink('/about/some/sub-page')).toEqual(false);
        });

    });

});

