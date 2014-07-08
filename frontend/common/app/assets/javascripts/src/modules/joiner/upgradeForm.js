define([
    '$',
    'bean',
    'ajax',
    'src/modules/joiner/form'
], function ($, bean, ajax, StripeForm) {

    function UpgradeForm () {}

    UpgradeForm.prototype.init = function () {

        var differentBillingAddressButton = $('.js-toggle-billing-address'),
            billingAddressFieldset = $('.js-billingAddress-fieldset');

        bean.on(differentBillingAddressButton[0], 'click', function () {
            billingAddressFieldset.toggleClass('u-h');
        });

        var stripe = new StripeForm();
        stripe.init(undefined, function (response, form) {
            var self = this;
            // token contains id, last4, and card type
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
    };

    return UpgradeForm;
});