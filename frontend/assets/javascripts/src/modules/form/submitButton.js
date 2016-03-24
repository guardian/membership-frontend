define(
    [
        '$',
        'lodash/string/template',
        'text!src/templates/checkout/submitButton.html'
    ],
    function($, template, submitButtonTemplate) {
        'use strict';

        var $SUBMIT_BUTTON = $('.js-submit-input-cta');

        function render() {
            if ($SUBMIT_BUTTON.length > 0) {
                $SUBMIT_BUTTON.html(template(submitButtonTemplate)(guardian.membership.checkoutForm));
            }
        }

        return {
            init: render,
            render: render
        };
    }
);
