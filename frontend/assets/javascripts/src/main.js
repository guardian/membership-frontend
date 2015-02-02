require([
    'src/utils/analytics/setup',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/info/Feedback',
    'src/modules/tier/PaidForm',
    'src/modules/tier/StaffForm',
    'src/modules/events/Cta',
    'src/modules/events/filter',
    'src/modules/events/toggle',
    'src/modules/slideshow',
    'src/modules/images',
    'src/modules/sticky',
    'src/modules/Header',
    'src/modules/navigation',
    'src/modules/UserDetails',
    'src/modules/tier/choose',
    'src/modules/events/eventPriceEnhance',
    'src/modules/patterns',
    'src/utils/modal',
    'src/utils/form/processSubmit',
    // Add new dependencies ABOVE this
    'raven',
    'modernizr'
], function(
    analytics,
    ajax,
    JoinFree,
    FeedbackForm,
    PaidForm,
    StaffForm,
    Cta,
    filter,
    toggle,
    slideshow,
    images,
    sticky,
    Header,
    navigation,
    UserDetails,
    choose,
    eventPriceEnhance,
    modal,
    patterns,
    processSubmit
) {
    'use strict';

    /*global Raven */
    // set up Raven, which speaks to Sentry to track errors
    Raven.config('https://e159339ea7504924ac248ba52242db96@app.getsentry.com/29912', {
        whitelistUrls: ['membership.theguardian.com/assets/'],
        tags: { build_number: guardian.membership.buildNumber }
    }).install();

    ajax.init({page: {ajaxUrl: ''}});

    analytics.init();

    // Global
    toggle.init();
    images.init();
    slideshow.init();
    sticky.init();
    (new Header()).init();
    navigation.init();
    (new UserDetails()).init();

    // Events
    (new Cta()).init();
    filter.init();
    eventPriceEnhance.init();

    // Join
    choose.init();
    (new JoinFree()).init();
    (new PaidForm()).init();
    (new StaffForm()).init();
    processSubmit.init();

    // Feedback
    (new FeedbackForm()).init();

    // Modal
    modal.init();

    // Pattern library
    patterns.init();

});
