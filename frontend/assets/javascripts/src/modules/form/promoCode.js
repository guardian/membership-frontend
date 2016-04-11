define(
    [
        '$',
        'ajax',
        'bean',
        'lodash/string/template',
        'src/modules/form/billingPeriodChoice',
        'text!src/templates/promoCode/promotion.html',
        'text!src/templates/promoCode/validationError.html'
    ],
    function ($, ajax, bean, template, billingPeriodChoice, promotionTemplate, validationErrorTemplate) {
        'use strict';

        var $PROMO_CODE_INPUT = $('#promo-code'),
            $DELIVERY_COUNTRY_SELECT = $('#country-deliveryAddress'),
            $BILLING_COUNTRY_SELECT = $('#country-billingAddress'),
            $TIER_ELEMENT = $('input[name="tier"]'),
            $APPLY_BUTTON = $('.js-promo-code-validate'),
            $FEEDBACK_CONTAINER = $('.js-promo-feedback-container');

        var isDiscountPromotion = function(promotion) {
            return !!promotion.promotionType.amount;
        };

        var isIncentivePromotion = function(promotion) {
            return promotion.promotionType.redemptionInstructions !== '';
        };

        var restoreOriginalRatePlans = function() {
            billingPeriodChoice.reset();
        };

        var showPromoCode = function(promotion) {
            if (isDiscountPromotion(promotion)) {
                promotion.appliesTo.productRatePlanIds.forEach(function(productRatePlanId) {
                    guardian.membership.checkoutForm.billingPeriods.choices.forEach(function(choice) {
                        if (choice.inputId === productRatePlanId) {
                            guardian.membership.checkoutForm.billingPeriods[choice.inputValue].discount = promotion.promotionType.amount;
                            choice.classes.push('pseudo-radio--promotion');
                            choice.promoted = true;
                        }
                    });
                });
                $FEEDBACK_CONTAINER.html(template(promotionTemplate)(promotion));
                billingPeriodChoice.render();
            } else if (isIncentivePromotion(promotion) || isDiscountPromotion(promotion)) {
                $FEEDBACK_CONTAINER.html(template(promotionTemplate)(promotion));
                restoreOriginalRatePlans();
            } else {
                // is a tracking promotion, so just leave the container blank.
                $FEEDBACK_CONTAINER.html('');
                restoreOriginalRatePlans();
            }
            $PROMO_CODE_INPUT.parent().removeClass('form-field--error');
            bindExtraKeyListener();
        };

        var showPromoError = function(ajax) {
            $FEEDBACK_CONTAINER.html(template(validationErrorTemplate)(JSON.parse(ajax.response)));
            $PROMO_CODE_INPUT.parent().addClass('form-field--error');
            restoreOriginalRatePlans();
        };

        var clearFeedbackContainer = function() {
            $FEEDBACK_CONTAINER.html('');
            $PROMO_CODE_INPUT.parent().removeClass('form-field--error');
            restoreOriginalRatePlans();
        };

        var validatePromoCode = function() {
            var trimmedCode = $PROMO_CODE_INPUT.val().trim();
            if(!trimmedCode) {
                clearFeedbackContainer();
                return;
            }

            return ajax({
                type: 'json',
                method: 'GET',
                url: '/lookupPromotion',
                data: {
                    promoCode: trimmedCode,
                    country: $DELIVERY_COUNTRY_SELECT.val(),
                    tier: $TIER_ELEMENT.val()
                }
            })
            .then(showPromoCode)
            .catch(showPromoError);
        };

        var bindExtraKeyListener = function() {
            if (bindExtraKeyListener.alreadyBound) {
                return;
            }
            bean.on($PROMO_CODE_INPUT[0], 'keyup blur', validatePromoCode);
            bindExtraKeyListener.alreadyBound = true;
        };

        return {
            init: function() {
                if ($PROMO_CODE_INPUT.length === 0) {
                    return;
                }
                // revalidate the code if we change / click stuff
                if ($DELIVERY_COUNTRY_SELECT.length > 0) {
                    bean.on($DELIVERY_COUNTRY_SELECT[0], 'change', validatePromoCode);
                }
                if ($BILLING_COUNTRY_SELECT.length > 0) {
                    bean.on($BILLING_COUNTRY_SELECT[0], 'change', validatePromoCode);
                }
                if ($APPLY_BUTTON.length > 0) {
                    bean.on($APPLY_BUTTON[0], 'click', validatePromoCode);
                }
                validatePromoCode();
            }
        };
    });
