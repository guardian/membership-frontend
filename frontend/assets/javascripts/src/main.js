require([
    'ajax',
    'src/modules/analytics/setup',
    'src/modules/events/cta',
    'src/modules/events/filter',
    'src/modules/slideshow',
    'src/modules/images',
    'src/modules/toggle',
    'src/modules/sticky',
    'src/modules/navigation',
    'src/modules/UserDetails',
    'src/modules/events/eventPriceEnhance',
    'src/modules/patterns',
    'src/modules/modal',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/metrics',
    'src/modules/menu',
    'src/modules/menuDetails',
    // Add new dependencies ABOVE this
    'raven',
    'modernizr'
], function(
    ajax,
    analytics,
    cta,
    filter,
    slideshow,
    images,
    toggle,
    sticky,
    navigation,
    UserDetails,
    eventPriceEnhance,
    modal,
    patterns,
    processSubmit,
    form,
    metrics,
    menu,
    menuDetails
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
    menu.init();
    menuDetails.init();
    navigation.init();
    (new UserDetails()).init();

    // Events
    cta.init();
    filter.init();
    eventPriceEnhance.init();

    // Forms
    form.init();
    processSubmit.init();

    // Modal
    modal.init();

    // Pattern library
    patterns.init();

    // Metrics
    metrics.init();

});
