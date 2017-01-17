require([
    'ajax',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/welcome',
    'src/modules/slideshow',
    'src/modules/images',
    'src/modules/toggle',
    'src/modules/dropdown',
    'src/modules/sticky',
    'src/modules/sectionNav',
    'src/modules/navigation',
    'src/modules/userDetails',
    'src/modules/videoOverlay',
    'src/modules/modal',
    'src/modules/events/cta',
    'src/modules/events/remainingTickets',
    'src/modules/events/eventPriceEnhance',
    'src/modules/filterFacets',
    'src/modules/filterLiveSearch',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/identityMenu/setup',
    'src/modules/comparisonTable',
    'src/modules/metrics',
    'src/modules/patterns',
    'src/modules/paidToPaid',
    'src/modules/memstatus',
    'src/modules/tools/priceABTest',
    'src/modules/faq'
], function(
    ajax,
    raven,
    analytics,
    welcome,
    slideshow,
    images,
    toggle,
    dropdown,
    sticky,
    sectionNav,
    navigation,
    userDetails,
    videoOverlay,
    modal,
    cta,
    remainingTickets,
    eventPriceEnhance,
    filterFacets,
    filterLiveSearch,
    form,
    processSubmit,
    identityMenu,
    comparisonTable,
    metrics,
    patterns,
    paidToPaid,
    memstatus,
    priceABTest,
    faq
) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});
    raven.init('https://8ad435f4fefe468eb59b19fd81a06ea9@app.getsentry.com/56405');

    //Price ABTest
    priceABTest.init();

    analytics.init();

    // Global
    welcome.init();
    slideshow.init();
    images.init();
    toggle.init();
    dropdown.init();
    sticky.init();
    sectionNav.init();
    identityMenu.init();
    navigation.init();
    userDetails.init();
    videoOverlay.init();
    modal.init();
    comparisonTable.init();

    // Events
    cta.init();
    remainingTickets.init();
    eventPriceEnhance.init();

    // Filtering
    filterFacets.init();
    filterLiveSearch.init();

    // Forms
    form.init();
    processSubmit.init();


    // Metrics
    metrics.init();

    // Pattern library
    patterns.init();

    paidToPaid.init();

    memstatus.init();

    faq.init();
});
