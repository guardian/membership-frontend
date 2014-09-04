require([
    'lib/bower-components/imager.js/Imager',
    'src/utils/analytics/omniture',
    'src/utils/router',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/tier/JoinPaid',
    'src/modules/info/Feedback',
    'src/modules/tier/Upgrade',
    'src/modules/events/Cta',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/tier/Choose',
    'src/utils/cookie',
    'src/modules/events/eventPriceEnhance',
    'config/appCredentials'
], function(
    Imager,
    omnitureAnalytics,
    router,
    ajax,
    JoinFree,
    JoinPaid,
    FeedbackForm,
    Upgrade,
    Cta,
    Header,
    DatetimeEnhance,
    Choose,
    cookie,
    eventPriceEnhance,
    appCredentials
    ) {
    'use strict';

    var MEM_USER_COOKIE_KEY = appCredentials.membership.userCookieKey;
    var header;

    ajax.init({page: {ajaxUrl: ''}});

    router.match('*').to(function () {
        header = new Header();
        header.init();
        omnitureAnalytics.init();

        /* jshint ignore:start */
        // avoid "Do not use 'new' for side effects" error
        new Imager({ availableWidths: [300, 460, 620, 940], availablePixelRatios: [1, 2] });
        /* jshint ignore:end */
    });

    router.match('/event/').to(function () {
        (new DatetimeEnhance()).init();
        (new Cta()).init();
        eventPriceEnhance.init();
    });

    router.match(['*/thankyou', '*/summary']).to(function () {
        // TODO potentially abstract this into its own class if user details stuff grows
        // user has upgraded or joined so remove cookie then populate the user details in the header
        cookie.removeCookie(MEM_USER_COOKIE_KEY);
        header.populateUserDetails();
    });

    router.match('*/friend/enter-details').to(function () {
        (new JoinFree()).init();
    });

    router.match(['*/payment', '*/partner/enter-details', '*/patron/enter-details']).to(function () {
        (new JoinPaid()).init();
    });

    router.match(['*/tier/change/partner', '*/tier/change/patron']).to(function () {
        (new Upgrade()).init();
    });

    router.match('/choose-tier').to(function () {
        (new Choose()).init();
    });

    router.match('/feedback').to(function () {
        (new FeedbackForm()).init();
    });

    /**
     * We were using domready here but for an unknown reason it is not firing in our production environment.
     * Please ask Ben Chidgey or Chris Finch if there are issues around this.
     */
    router.go();
});
