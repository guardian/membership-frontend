require([
    'src/utils/analytics/omniture',
    'src/utils/router',
    'domready',
    'ajax',
    'src/modules/joiner/form',
    'src/modules/events/ctaButton',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/events/modifyEvent'
], function(omnitureAnalytics, router, domready, ajax, StripeForm, ctaButton, Header, DatetimeEnhance, modifyEvent) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event').to(function () {
        (new DatetimeEnhance()).init();
        ctaButton.init();
        modifyEvent.init();
    });

    router.match('*/payment').to(function () {
        (new StripeForm()).init();
    });

    router.match('*').to(function () {
        (new Header()).init();
        omnitureAnalytics.init();
    });

    domready(function() {
        router.go();
    });

});