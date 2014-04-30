require.config({
    paths: {
        'jquery': '//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min',
        'jQueryPayment': 'components/stripe/jquery.payment',
        'stripe': 'https://js.stripe.com/v2/?',
        'eventsForm': 'modules/events/forms',
        'user': 'utils/user',
        'config': 'config/config'
    },
    shim: {
        'payment': {
            deps: ['jquery']
        }
    }
});


require([
    'eventsForm'
], function(eventsForm){

    'use strict';

    eventsForm.init();

});