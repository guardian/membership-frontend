require([
    'lib/bower-components/imager.js/dist/Imager.min',
    'src/utils/analytics/omniture',
    'src/utils/router',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/tier/JoinPaid',
    'src/modules/tier/Upgrade',
    'src/modules/events/Cta',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/events/modifyEvent'
], function(
    Imager,
    omnitureAnalytics,
    router,
    ajax,
    JoinFree,
    JoinPaid,
    Upgrade,
    Cta,
    Header,
    DatetimeEnhance,
    modifyEvent
    ) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event/').to(function () {
        (new DatetimeEnhance()).init();
        (new Cta()).init();
        modifyEvent.init();
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

    router.match('*').to(function () {
        (new Header()).init();
        omnitureAnalytics.init();

        return new Imager({ availableWidths: [300, 460], availablePixelRatios: [1, 2] }); // http://stackoverflow.com/questions/3686267/how-do-i-address-the-jslint-warning-do-not-use-new-for-side-effects
    });

    /**
     * We were using domready here but for an unknown reason it is not firing in our production environment.
     * Please ask Ben Chidgey or Chris Finch if there are issues around this.
     */
    router.go();
});
