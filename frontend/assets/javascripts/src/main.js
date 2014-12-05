require([
    '$',
    'lib/bower-components/imager.js/Imager',
    'src/utils/analytics/setup',
    'src/utils/cookieRefresh',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/info/Feedback',
    'src/modules/tier/PaidForm',
    'src/modules/events/Cta',
    'src/modules/events/filter',
    'src/modules/events/toggle',
    'src/modules/sticky',
    'src/modules/Header',
    'src/modules/UserDetails',
    'src/modules/tier/choose',
    'src/modules/events/eventPriceEnhance',
    'src/modules/tier/Thankyou',
    'src/modules/patterns',
    'src/utils/addToClipboard',
    'lib/bower-components/raven-js/dist/raven', // add new deps ABOVE this
    'src/utils/modernizr'
], function(
    $,
    Imager,
    analytics,
    cookieRefresh,
    ajax,
    JoinFree,
    FeedbackForm,
    PaidForm,
    Cta,
    Filter,
    toggle,
    sticky,
    Header,
    UserDetails,
    choose,
    eventPriceEnhance,
    Thankyou,
    patterns,
    addToClipboard
) {
    'use strict';

    /*global Raven */
    // set up Raven, which speaks to Sentry to track errors
    Raven.config('https://e159339ea7504924ac248ba52242db96@app.getsentry.com/29912', {
        whitelistUrls: ['membership.theguardian.com/assets/'],
        tags: { build_number: guardian.membership.buildNumber }
    }).install();

    ajax.init({page: {ajaxUrl: ''}});

    // event imagery
    if ($('.delayed-image-load').length) {
        new Imager('.delayed-image-load', {
            availableWidths: guardian.membership.eventImages.widths,
            availablePixelRatios: guardian.membership.eventImages.ratios,
            lazyload: true,
            lazyloadOffset: 100
        });
    }
    // home page hero (a-b) imagery
    if ($('.delayed-home-image-load').length) {
        new Imager('.delayed-home-image-load', {
            availableWidths: guardian.membership.homeImages.widths,
            availablePixelRatios: guardian.membership.homeImages.ratios,
            lazyload: true,
            lazyloadOffset: 100
        });
    }
    // home page promo (a-b) imagery
    if ($('.delayed-home-promo-image-load').length) {
        new Imager('.delayed-home-promo-image-load', {
            availableWidths: guardian.membership.homeImages.promoWidths,
            availablePixelRatios: guardian.membership.homeImages.ratios,
            lazyload: true,
            lazyloadOffset: 100
        });
    }

    // TODO: Remove this, see module
    cookieRefresh.init();

    analytics.init();

    // Global
    toggle.init();
    sticky.init();
    var header = new Header();
    header.init();
    (new UserDetails()).init();

    // Events
    (new Cta()).init();
    eventPriceEnhance.init();

    // Join
    choose.init();
    (new JoinFree()).init();
    (new PaidForm()).init();
    (new Thankyou()).init(header);

    // Feedback
    (new FeedbackForm()).init();

    // Pattern library
    patterns.init();

    addToClipboard.init();
});
