define(
    [
        '$',
        'lodash/string/template',
        'text-loader!src/templates/checkout/ongoingCardPayments.html'
    ],
    function($, template, ongoingCardPaymentsTemplate) {
        'use strict';

        var $ONGOING_CARD_PAYMENTS = $('.js-ongoing-card-payments');

        function render() {
            if ($ONGOING_CARD_PAYMENTS.length > 0) {
                $ONGOING_CARD_PAYMENTS.html(template(ongoingCardPaymentsTemplate)(guardian.membership.checkoutForm));
            }
        }

        return {
            init: render,
            render: render
        };
    }
);
