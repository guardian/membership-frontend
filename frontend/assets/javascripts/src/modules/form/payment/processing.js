/*global Stripe */
define([
    'ajax',
    'src/utils/helper',
    'src/modules/form/helper/formUtil',
    'src/modules/form/helper/serializer',
    'src/modules/form/helper/loader',
    'config/paymentErrorMessages',
    'src/modules/form/validation/display',
    'src/modules/raven'
], function (ajax, utilsHelper, formUtil, serializer, loader, paymentErrorMessages, display,raven) {
    'use strict';

    var Raven = raven.Raven;
    var CREDIT_CARD_NUMBER_ELEM = document.querySelector('.js-credit-card-number');
    var CREDIT_CARD_CVC_ELEM = document.querySelector('.js-credit-card-cvc');
    var CREDIT_CARD_MONTH_ELEM = document.querySelector('.js-credit-card-exp-month');
    var CREDIT_CARD_YEAR_ELEM = document.querySelector('.js-credit-card-exp-year');

    /**
     * populate the stripe token
     */
    var getStripeToken = function () {
        Stripe.card.createToken({
            number: CREDIT_CARD_NUMBER_ELEM.value,
            cvc: CREDIT_CARD_CVC_ELEM.value,
            exp_month: CREDIT_CARD_MONTH_ELEM.value,
            exp_year: CREDIT_CARD_YEAR_ELEM.value
        }, stripeResponseHandler);
    };

    /**
     * stripe response handler
     *
     * call jsonp stripe.card.createToken:
     *   ON ERROR:
     *    - displaying relevant error message
     *    - stop loader
     *    - reset processing message
     *    - enable the submit button
     *   ON SUCCESS:
     *    - set processing message
     *    - make ajax call to membership backend
     *      ON SUCCESS:
     *       - redirect
     *      ON ERROR:
     *      - displaying relevant error message
     *      - stop loader
     *      - reset processing message
     *      - enable the submit button
     *
     * @param status
     * @param response
     */
    var stripeResponseHandler = function (status, response) {
        var data;
        if (response.error) {
            Raven.captureMessage(response.error.type + '; ' + response.error.code + '; ' + response.error.message + '; ' + response.error.param);
            var userMessage = paymentErrorMessages.getMessage(response.error);
            var errorElement = paymentErrorMessages.getElement(response.error);
            if (userMessage) {
                handleError(userMessage, errorElement);
            }
        } else {
            data = serializer(utilsHelper.toArray(formUtil.elem.elements), { 'payment.token': response.id });

            loader.setProcessingMessage('Checking card details...');

            ajax({
                url: formUtil.elem.action,
                method: 'post',
                data: data,
                success: function (successData) {
                    if (typeof successData === 'undefined' || typeof successData.redirect === 'undefined') {
                        Raven.captureMessage('Empty successData received');
                    }
                    window.location.assign(successData.redirect);
                },
                error: function (err) {
                    var paymentErr;

                    try {
                        paymentErr = JSON.parse(err.response);
                    } catch (e) {
                        Raven.captureException(e);
                    }

                    var userMessage = paymentErrorMessages.getMessage(paymentErr);
                    var errorElement = paymentErrorMessages.getElement(paymentErr);
                    handleError(userMessage, errorElement);
                }
            });

        }
    };

    /**
     * - displaying relevant error message
     * - stop loader
     * - reset processing message
     * - enable the submit button
     * @param msg
     */
    var handleError = function (msg, elem) {
        display.toggleErrorState({
            isValid: false,
            elem: elem,
            msg: msg
        });

        loader.stopLoader();
        loader.setProcessingMessage('');
        loader.disableSubmitButton(false);
    };

    return {
        getStripeToken: getStripeToken
    };
});
