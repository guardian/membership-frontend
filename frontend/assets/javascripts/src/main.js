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
    'src/modules/userDetails',
    'src/modules/videoOverlay',
    'src/modules/events/eventPriceEnhance',
    'src/modules/modal',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/metrics',
    'src/modules/patterns',
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
    userDetails,
    videoOverlay,
    eventPriceEnhance,
    modal,
    processSubmit,
    form,
    identityPopup,
    identityPopupDetails,
    metrics,
    patterns
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
    identityPopup.init();
    identityPopupDetails.init();
    navigation.init();
    userDetails.init();
    videoOverlay.init();

    // Events
    cta.init();
    filter.init();
    eventPriceEnhance.init();

    // Forms
    form.init();
    processSubmit.init();

    // Modal
    modal.init();

    // Metrics
    metrics.init();

    // Pattern library
    patterns.init();

});
