require([
    'src/utils/router',
    'domready',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account',
    'src/modules/events/datetimeEnhance'
], function(router, domready, stripeForm, ctaButton, account, datetimeEnhance){
    'use strict';

//    router.match('/events').to(function () {
//        console.log('events page');
//    });

    router.match('/event').to(function () {
        ctaButton.init();
        datetimeEnhance.init();
    });

    router.match([
        '/stripe',
        '/partner-registration'
    ]).to(stripeForm.init);

    router.match('*').to(function () {
        account.init();
    });

    domready(function () {
        router.go();
    });

});