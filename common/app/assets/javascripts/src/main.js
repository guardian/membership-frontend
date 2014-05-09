require([
    'domready',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account',
    'src/modules/events/datetimeEnhance'
], function(domready, stripeForm, ctaButton, account, datetimeEnhance){
    'use strict';

    domready( function(){

        // Shoddy router

        var path = document.location.pathname;

        if (path.match(/^\/event/)) {
            ctaButton.init();
            datetimeEnhance.init();
        //} else if (path.match(/^\/events/)) {

        } else if (path.match(/^\/stripe/)) {
            stripeForm.init();
        } else {
            account.init();
        }

    });
});