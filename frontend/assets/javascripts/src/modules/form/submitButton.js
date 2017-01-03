define(
    [
        '$',
        'lodash/string/template',
        'text!src/templates/checkout/submitButton.html'
    ],
    function($, template, submitButtonTemplate) {
        'use strict';

        var $SUBMIT_BUTTON = $('.js-submit-input');
        var $SUBMIT_SPAN = $('.js-submit-input-cta');

        function render(payload) {
            if ($SUBMIT_SPAN.length > 0) {
                $SUBMIT_SPAN.html(template(submitButtonTemplate)(guardian.membership.checkoutForm));
                if(payload) {
                    $SUBMIT_BUTTON[0].setAttribute('data-metric-label', 'payment ' + payload);
                }
            }
        }

        return {
            init: render,
            render: render
        };
    }
);
