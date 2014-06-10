require([
    'src/utils/router',
    'domready',
    'ajax',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account',
    'src/modules/events/datetimeEnhance',
    'src/modules/events/modifyEvent'
], function(router, domready, ajax, StripeForm, ctaButton, account, DatetimeEnhance, modifyEvent) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event').to(function () {
        var dateEnhance = new DatetimeEnhance();
        dateEnhance.init();
        ctaButton.init();
        modifyEvent.init();
    });

    router.match('*/payment').to(function () {
        var stripe = new StripeForm();
        stripe.init();
    });

    router.match('*').to(function () {
        account.init();
    });

    domready(function() {
        router.go();
    });

});