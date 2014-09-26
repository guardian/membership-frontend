require([
    'lib/bower-components/imager.js/Imager',
    'src/utils/analytics/omniture',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/info/Feedback',
    'src/modules/tier/PaidForm',
    'src/modules/events/Cta',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/tier/Choose',
    'src/modules/events/eventPriceEnhance',
    'src/modules/tier/Thankyou',
    'lib/bower-components/raven-js/dist/raven', // add new deps ABOVE this
    'src/utils/modernizr'
], function(
    Imager,
    omnitureAnalytics,
    ajax,
    JoinFree,
    FeedbackForm,
    PaidForm,
    Cta,
    Header,
    DatetimeEnhance,
    Choose,
    eventPriceEnhance,
    Thankyou
) {
    'use strict';

    /*global Raven */
    // set up Raven, which speaks to Sentry to track errors
    Raven.config('https://e159339ea7504924ac248ba52242db96@app.getsentry.com/29912', {
        whitelistUrls: ['membership.theguardian.com/assets/'],
        tags: { build_number: guardian.membership.buildNumber }
    }).install();

    ajax.init({page: {ajaxUrl: ''}});

    /* jshint ignore:start */
    // avoid "Do not use 'new' for side effects" error
    // these values are defined in application.conf
    new Imager({
        availableWidths: guardian.membership.eventImages.widths,
        availablePixelRatios: guardian.membership.eventImages.ratios
    });
    /* jshint ignore:end */

    require('ophan/ng', function () {});

    // Global
    var header = new Header();
    header.init();
    omnitureAnalytics.init();

    // Events
    (new DatetimeEnhance()).init();
    (new Cta()).init();
    eventPriceEnhance.init();

    // Join
    (new Choose()).init();
    (new JoinFree()).init();
    (new PaidForm()).init();
    (new Thankyou()).init(header);

    // Feedback
    (new FeedbackForm()).init();
});
