require([
    'src/utils/analytics/omniture',
    'src/utils/router',
    'domready',
    'ajax',
    'src/modules/joiner/form',
    'src/modules/events/ctaButton',
    'src/modules/Header',
    'src/modules/events/DatetimeEnhance',
    'src/modules/events/modifyEvent'
], function(omnitureAnalytics, router, domready, ajax, StripeForm, ctaButton, Header, DatetimeEnhance, modifyEvent) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    router.match('/event').to(function () {
        (new DatetimeEnhance()).init();
        ctaButton.init();
        modifyEvent.init();
    });

    router.match('*/payment').to(function () {
        var stripe = new StripeForm();
        stripe.init(undefined, function (response) {
            var self = this;
            // token contains id, last4, and card type
            var token = response.id;

            ajax({
                url: '/subscription/subscribe',
                method: 'post',
                data: {
                    stripeToken: token,
                    tier: self.getElement('TIER_FIELD').val()
                },
                success: function () {
                    self.stopLoader();
                    window.location = window.location.href.replace('payment', 'thankyou');
                },
                error: function (error) {

                    var errorObj,
                        errorMessage;

                    try {
                        errorObj = error.response && JSON.parse(error.response);
                        errorMessage = self.getErrorMessage(errorObj);
                        if (errorMessage) {
                            self.handleErrors([errorMessage]);
                        }
                    } catch (e) {}

                    self.stopLoader();
                }
            });
        });
    });

    router.match(['*/tier/change/partner', '*/tier/change/patron']).to(function () {
        var stripe = new StripeForm();
        stripe.init(undefined, function (response, form) {
            var self = this;
            // token contains id, last4, and card type...
            var token = response.id;

            var toTier = form.getAttribute('data-change-to-tier').toLowerCase();

            var inputs = Array.prototype.slice.call(form.querySelectorAll('input[name]'))
                            .concat(Array.prototype.slice.call(form.querySelectorAll('select[name]')));

            var serializedForm = {
                stripeToken: token
            };

            for (var i=0; i<inputs.length; i++) {
                var input = inputs[i];
                if (input.type !== 'radio' || input.checked) {
                    serializedForm[input.name] = input.value;
                }
            }

            ajax({
                url: '/tier/change/' + toTier,
                method: 'post',
                data: serializedForm,
                success: function () {
                    self.stopLoader();

                    window.location = window.location.href = '/tier/change/' + toTier + '/summary';
                },
                error: function (error) {

                    var errorObj,
                        errorMessage;

                    try {
                        errorObj = error.response && JSON.parse(error.response);
                        errorMessage = self.getErrorMessage(errorObj);
                        if (errorMessage) {
                            self.handleErrors([errorMessage]);
                        }
                    } catch (e) {}

                    self.stopLoader();
                }
            });
        });
    });

    router.match('*').to(function () {
        (new Header()).init();
        omnitureAnalytics.init();
    });

    domready(function() {
        router.go();
    });

});