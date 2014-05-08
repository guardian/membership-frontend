require([
    'domready',
    'src/modules/events/form',
    'src/modules/events/ctaButton',
    'src/modules/account'
], function(domready, PaymentForm, ctaButton, account){
    'use strict';

    domready( function(){

        var stripeForm = new PaymentForm();

        stripeForm.init();
        ctaButton.init();
        account.init();
    });
});