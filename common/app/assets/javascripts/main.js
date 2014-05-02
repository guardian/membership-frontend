require([
    'domready',
    //'eventsForm',
    'ctaButton',
    'modules/account'
], function(domready, /*eventsForm,*/ ctaButton, account){
    'use strict';

    domready( function(){

        console.log( 'dom loaded' );

       // eventsForm.init();
        ctaButton.init();
        account.init();
    });
});