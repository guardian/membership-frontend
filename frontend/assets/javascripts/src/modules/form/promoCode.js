define(
    [
        '$',
        'ajax',
        'bean',
        'lodash/string/template',
        'text!src/templates/promoCode.html',
        'text!src/templates/promoError.html'
    ],
    function ($, ajax, bean, template, promoCodeTemplate,promoErrorTemplate) {
        'use strict';

        var COUNTRY_SELECT = '.js-country',
            PROMO_CODE_INPUT = '#promo-code',
            TIER_ELEMENT = 'input[name="tier"]',
            APPLY_BUTTON = '.js-promo-code-validate',
            BILLING_PERIOD = '.js-payment-options-container [type=radio]:checked',
            FEEDBACK_CONTAINER = '.js-promo-feedback-container';

        var elementsThatTriggerRevalidation = $([
            COUNTRY_SELECT, BILLING_PERIOD, APPLY_BUTTON
        ].join(','));

        var showPromoCode = function(promotion) {
            $(FEEDBACK_CONTAINER).html(template(promoCodeTemplate)(promotion));
        };

        var showPromoError = function(error) {
            $(FEEDBACK_CONTAINER).html(template(promoErrorTemplate)(error));
            $(PROMO_CODE_INPUT).parent().addClass('form-field--error');
        };

        var clearFeedbackContainer = function() {
            $(FEEDBACK_CONTAINER).html('');
            $(PROMO_CODE_INPUT).parent().removeClass('form-field--error');
        };

        var validatePromoCode = function() {

            clearFeedbackContainer();
            if(!$(PROMO_CODE_INPUT).val().trim()) {
                return;
            }

            ajax({
                type: 'json',
                method: 'GET',
                url: '/lookupPromotion',
                data: {
                    promoCode: $(PROMO_CODE_INPUT).val(),
                    billingPeriod: $(BILLING_PERIOD).val(),
                    country: $(COUNTRY_SELECT).val(),
                    tier: $(TIER_ELEMENT).val()
                }
            }).then(showPromoCode)
              .catch(function (a) {
                showPromoError(JSON.parse(a.response))
            });
        };

        return {
            init: function() {
                if (!$(PROMO_CODE_INPUT).length) {
                    return;
                }

                // clear the existing promo code when we are editing it
                bean.on($(PROMO_CODE_INPUT)[0], 'keyup', clearFeedbackContainer);

                // revalidate the code if we change / click stuff
                elementsThatTriggerRevalidation.each(function(elem) {
                    bean.on(elem, elem.tagName.toLowerCase() == 'button' ? 'click' : 'change', validatePromoCode);
                });

                //handle pre-population of codes
                if ($(PROMO_CODE_INPUT).val()) {
                    validatePromoCode()
                }
            }
        }
    });
