require([
    'src/utils/router',
    'domready',
    'ajax',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account',
    'src/modules/account',
    'src/modules/events/datetimeEnhance'
], function(router, domready, ajax, StripeForm, ctaButton, account, datetimeEnhance){
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event').to(function () {
        ctaButton.init();
        datetimeEnhance.init();
    });

    router.match('*/payment').to(function () {
        var stripe = new StripeForm();
        stripe.init();
    });

    router.match('*').to(function () {
        account.init();
    });

    domready(function(){
        router.go();
    });

});