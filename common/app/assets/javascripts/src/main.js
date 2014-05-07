require([
    'domready',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account'
], function(domready, stripeForm, ctaButton, account){
    'use strict';

    domready( function(){

        stripeForm.init();
        ctaButton.init();
        account.init();
    });
});