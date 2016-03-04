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

        var showPromoError = function(ajax) {
            $(FEEDBACK_CONTAINER).html(template(promoErrorTemplate)(JSON.parse(ajax.response)));
            $(PROMO_CODE_INPUT).parent().addClass('form-field--error');
        };

        var clearFeedbackContainer = function() {
            $(FEEDBACK_CONTAINER).html('');
            $(PROMO_CODE_INPUT).parent().removeClass('form-field--error');
        };

        var validatePromoCode = function(code) {

            var trimmedCode = code.trim();
            clearFeedbackContainer();

            if(!trimmedCode) {
                return {
                    // a DIY pending promise
                    then: function() { return this },
                    catch: function() { return this }
                };
            }

            return ajax({
                type: 'json',
                method: 'GET',
                url: '/lookupPromotion',
                data: {
                    promoCode: trimmedCode,
                    billingPeriod: $(BILLING_PERIOD).val(),
                    country: $(COUNTRY_SELECT).val(),
                    tier: $(TIER_ELEMENT).val()
                }
            })
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
                    bean.on(elem, elem.tagName.toLowerCase() == 'button' ? 'click' : 'change', function() {
                        validatePromoCode($(PROMO_CODE_INPUT).val()).then(showPromoCode).catch(showPromoError)
                    });
                });

                //handle pre-population of codes
                var prepopulatedCode = $(PROMO_CODE_INPUT).val();

                if (prepopulatedCode) {
                    $(PROMO_CODE_INPUT).val('');
                    validatePromoCode(prepopulatedCode).then(showPromoCode).then(function() {
                        $(PROMO_CODE_INPUT).val(prepopulatedCode)
                    });
                }
            }
        }
    });
