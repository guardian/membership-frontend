require([
    'domready',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account'
], function(domready, paymentForm, ctaButton, account){
    'use strict';

    domready( function(){

        var stripeForm = new paymentForm();

        stripeForm.init();
        ctaButton.init();
        account.init();
    });
});