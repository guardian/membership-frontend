/*global require */

require.config({
    paths: {
        // 'config': '../app/config',
        'jquery': '//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min',
        'payment': 'lib/jquery.payment',
        'stripe': 'https://js.stripe.com/v2/?'
    },
    shim: {
        'payment': {
            deps: ['jquery']
        }
    }
});

require([
    'jquery',
    'payment',
    'stripe'
], function($, Payment, Stripe) {

    "use strict";

    var config = {
        stripePublishableKey: 'pk_test_Qm3CGRdrV4WfGYCpm0sftR0f'
    };

    var stripeResponseHandler = function (status, response) {
        var $form = $('#payment-form');

        if (response.error) {
            // Show the errors on the form
            $form.find('.payment-errors').text(response.error.message);
            $form.find('button').prop('disabled', false);
        } else {
            // token contains id, last4, and card type
            var token = response.id;
            // Insert the token into the form so it gets submitted to the server
            $form.append($('<input type="hidden" name="stripeToken" />').val(token));
            // and submit
            $form.get(0).submit();
        }
    };

    var init = function () {
        Stripe.setPublishableKey(config.stripePublishableKey);

        $('#payment-form').submit(function(event) {
            event.preventDefault();
            var $form = $(this);

            // Disable the submit button to prevent repeated clicks
            $form.find('button').prop('disabled', true);

            Stripe.card.createToken($form, stripeResponseHandler);
        });
    };

    $(init);
});
