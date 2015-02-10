/*global Raven */
define([
    'ajax',
    'stripe',
    'src/utils/helper',
    'src/modules/form/helper/formUtil',
    'src/modules/form/helper/serializer',
    'src/modules/form/helper/loader',
    'config/paymentErrorMessages',
    'src/modules/form/validation/display'
], function (ajax, stripe, utilsHelper, form, serializer, loader, paymentErrorMessages, display) {
    'use strict';

    var CREDIT_CARD_NUMBER_ELEM = document.querySelector('.js-credit-card-number');
    var CREDIT_CARD_CVC_ELEM = document.querySelector('.js-credit-card-cvc');
    var CREDIT_CARD_MONTH_ELEM = document.querySelector('.js-credit-card-exp-month');
    var CREDIT_CARD_YEAR_ELEM = document.querySelector('.js-credit-card-exp-year');

    /**
     * populate the stripe token
     */
    var getStripeToken = function () {
        stripe.card.createToken({
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
        var data, errMsg;
        if (response.error) {
            errMsg = paymentErrorMessages.getMessage(response.error);
            if (errMsg) {
                handleError(errMsg);
            }
        } else {
            data = serializer(utilsHelper.toArray(form.elem.elements), { 'payment.token': response.id });

            loader.setProcessingMessage('Making payment...');

            ajax({
                url: form.elem.action,
                method: 'post',
                data: data,
                success: function (data) {
                    window.location.assign(data.redirect);
                },
                error: function (err) {
                    var paymentErr;

                    try {
                        paymentErr = JSON.parse(err.response);
                    } catch (e) {
                        Raven.captureException(e);
                    }

                    handleError(paymentErrorMessages.getMessage(paymentErr));
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
    var handleError = function (msg) {
        display.toggleErrorState({
            isValid: false,
            elem: CREDIT_CARD_NUMBER_ELEM,
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
