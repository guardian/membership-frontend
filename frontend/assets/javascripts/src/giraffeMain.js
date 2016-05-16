require([
    'ajax',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/dropdown',
    'src/modules/navigation',
    'src/modules/userDetails',
    'src/modules/events/cta',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/metrics',
    'src/modules/giraffe'
], function(
    ajax,
    raven,
    analytics,
    images,
    dropdown,
    navigation,
    userDetails,
    cta,
    form,
    processSubmit,
    identityPopup,
    identityPopupDetails,
    metrics,
    giraffe) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});
    raven.init('https://8ad435f4fefe468eb59b19fd81a06ea9@app.getsentry.com/56405');

    analytics.init();

    // Global
    images.init();
    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    navigation.init();
    userDetails.init();

    // Events
    cta.init();

    // Forms
    form.init();
    processSubmit.init();

    // Metrics
    metrics.init();



    giraffe.init();
});
