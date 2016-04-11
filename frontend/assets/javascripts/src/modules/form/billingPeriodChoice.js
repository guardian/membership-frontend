define(
    [
        '$',
        'bean',
        'lodash/string/template',
        'src/modules/form/submitButton',
        'src/modules/form/ongoingCardPayments',
        'text!src/templates/checkout/billingPeriodChoice.html'
    ],
    function($, bean, template, submitButton, ongoingCardPayments, billingPeriodChoiceTemplate) {
        'use strict';

        var checkoutForm = guardian.membership.checkoutForm,
            billingPeriods = guardian.membership.checkoutForm.billingPeriods,
            $BILLING_PERIOD_CONTAINER = $('.js-billing-period__container');

        function notifyOtherModulesToRender() {
            ongoingCardPayments.render();
            submitButton.render();
        }

        function render() {
            if (billingPeriods && $BILLING_PERIOD_CONTAINER.length > 0) {
                $BILLING_PERIOD_CONTAINER.html(template(billingPeriodChoiceTemplate)(checkoutForm));
                notifyOtherModulesToRender();
            }
        }

        function reset() {
            if (billingPeriods) {
                billingPeriods.annual.discount = 0;
                billingPeriods.annual.discountDurationMonths = 0;
                billingPeriods.monthly.discount = 0;
                billingPeriods.monthly.discountDurationMonths = 0;
                billingPeriods.choices.forEach(function (choice) {
                    choice.classes = [];
                    choice.promoted = false;
                });
                render();
            }
        }

        return {
            init: function() {
                if (billingPeriods && $BILLING_PERIOD_CONTAINER.length > 0) {
                    bean.on($BILLING_PERIOD_CONTAINER[0], 'change', '[type=radio]', function (el) {
                        el.target.checked = true;
                        checkoutForm.billingPeriod = el.target.value;
                        notifyOtherModulesToRender();
                    });
                    render();
                }
            },
            render: render,
            reset: reset
        };
    }
);
