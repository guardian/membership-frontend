require([
    'src/utils/analytics/omniture',
    'src/utils/router',
    'domready',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/tier/JoinPaid',
    'src/modules/tier/Upgrade',
    'src/modules/events/ctaButton',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/events/modifyEvent'
], function(
    omnitureAnalytics,
    router,
    domready,
    ajax,
    JoinFree,
    JoinPaid,
    Upgrade,
    ctaButton,
    Header,
    DatetimeEnhance,
    modifyEvent
    ) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event').to(function () {
        (new DatetimeEnhance()).init();
        ctaButton.init();
        modifyEvent.init();
    });

    router.match('*/detail').to(function () {
        (new JoinFree()).init();
    });

    router.match('*/payment').to(function () {
        (new JoinPaid()).init();
    });

    router.match(['*/tier/change/partner', '*/tier/change/patron']).to(function () {
        (new Upgrade()).init();
    });

    router.match('*').to(function () {
        (new Header()).init();
        omnitureAnalytics.init();
    });

    domready(function() {
        router.go();
    });

});
