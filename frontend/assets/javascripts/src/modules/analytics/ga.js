define(function() {
    'use strict';

    return {
        init: function() {
            /* Google analytics snippet */
            /*eslint-disable */
            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
            })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

            ga('create', guardian.googleAnalytics.trackingId, {
                'allowLinker': true,
                'cookieDomain': guardian.googleAnalytics.cookieDomain
            });

            ga('require', 'linker');
            ga('linker:autoLink', ['eventbrite.co.uk'] );
            ga('send', 'pageview');
            /*eslint-enable */
        }
    };
});
