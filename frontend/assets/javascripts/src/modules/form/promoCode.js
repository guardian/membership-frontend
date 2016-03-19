define(
    [
        '$',
        'ajax',
        'bean',
        'lodash/string/template',
        'text!src/templates/promoCode/promotion.html',
        'text!src/templates/promoCode/discountedRatePlan.html',
        'text!src/templates/promoCode/validationError.html'
    ],
    function ($, ajax, bean, template, promotionTemplate, discountedRatePlanTemplate, validationErrorTemplate) {
        'use strict';

        var $PROMOTED_RATE_PLAN,
            $PROMO_CODE_INPUT = $('#promo-code'),
            $COUNTRY_SELECT = $('.js-country'),
            $TIER_ELEMENT = $('input[name="tier"]'),
            $APPLY_BUTTON = $('.js-promo-code-validate'),
            $FEEDBACK_CONTAINER = $('.js-promo-feedback-container');

        var isDiscountPromotion = function(promotion) {
            return !!promotion.promotionType.amount;
        };

        var isForJustOneRatePlan = function(promotion) {
            return promotion.appliesTo.productRatePlanIds.length === 1;
        };

        var isIncentivePromotion = function(promotion) {
            return promotion.promotionType.redemptionInstructions !== '';
        };

        var restoreOriginalRatePlans = function() {
            if ($PROMOTED_RATE_PLAN && $PROMOTED_RATE_PLAN.original) {
                $PROMOTED_RATE_PLAN.html($PROMOTED_RATE_PLAN.original);
                $PROMOTED_RATE_PLAN.removeClass('pseudo-radio--promotion');
                delete $PROMOTED_RATE_PLAN.original;
            }
        };

        var showPromoCode = function(promotion) {
            if (isDiscountPromotion(promotion) && isForJustOneRatePlan(promotion)) {
                if (!($PROMOTED_RATE_PLAN && $PROMOTED_RATE_PLAN.original)) {
                    $PROMOTED_RATE_PLAN = $('#' + promotion.appliesTo.productRatePlanIds[0]);
                    $PROMOTED_RATE_PLAN.original = $PROMOTED_RATE_PLAN.html();
                }
                $PROMOTED_RATE_PLAN.html(template(discountedRatePlanTemplate)(promotion));
                $PROMOTED_RATE_PLAN.addClass('pseudo-radio--promotion');
                $FEEDBACK_CONTAINER.html(template(promotionTemplate)(promotion));
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
                    country: $COUNTRY_SELECT.val(),
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

                // revalidate the code if we change / click stuff
                bean.on($COUNTRY_SELECT[0], 'change', validatePromoCode);
                bean.on($APPLY_BUTTON[0], 'click', validatePromoCode);

                //handle pre-population of codes
                if ($PROMO_CODE_INPUT.val()) {
                    validatePromoCode();
                }
            }
        };
    });
