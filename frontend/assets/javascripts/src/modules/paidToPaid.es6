import html from 'text-loader!src/templates/promoCode/promotion.html'
import err from 'text-loader!src/templates/promoCode/validationError.html'
import template from 'lodash/template'
import ajax from 'ajax'
import bean from 'bean'
import $ from '$'

function combineCurrency(obj) {
    return {amount: obj.currency + obj.amount.toFixed(2)}
}


export function init() {

    const PROMO_CODE = $('.js-promocode-value');
    const FEEDBACK_CONTAINER = $('.js-promo-feedback-container');
    const PROMO_CODE_APPLY = $('.js-promo-code-apply');
    const FIRST_PAYMENT = $('.js-first-payment');
    const NEXT_PAYMENT = $('.js-next-payment');
    const LOADER = $('.js-promo-loader');

    if (!FIRST_PAYMENT.length) {
        return;
    }

    const NORMAL_PRICES = {
        firstPayment: {amount: FIRST_PAYMENT.text()},
        nextPayment: {amount: NEXT_PAYMENT.text()}
    };

    const promoTemplate = template(html);
    const promoError = template(err);

    function validateCode(code) {
        if (!code.trim()) {
            return;
        }

        LOADER.addClass('is-loading');

        ajax({
            type: 'json',
            url: window.location.pathname.replace('change', 'preview'),
            data: {promoCode: PROMO_CODE.val()}
        }).then(result => {
            LOADER.removeClass('is-loading');

            if (result.error) {
                FEEDBACK_CONTAINER.html(promoError({errorMessage: result.error}));
                return;
            }

            let prices = result.summary ? {
                firstPayment: combineCurrency(result.summary.targetSummary.firstPayment),
                nextPayment: combineCurrency(result.summary.targetSummary.nextPayment)
            } : NORMAL_PRICES;

            FIRST_PAYMENT.text(prices.firstPayment.amount);
            NEXT_PAYMENT.text(prices.nextPayment.amount);

            let promoMessage = result.promotion ? promoTemplate(result.promotion) : '';
            FEEDBACK_CONTAINER.html(promoMessage);
            LOADER.removeClass('is-loading');
        })
    }

    bean.on(PROMO_CODE[0], 'keyup', () => {
        FIRST_PAYMENT.text(NORMAL_PRICES.firstPayment.amount);
        NEXT_PAYMENT.text(NORMAL_PRICES.nextPayment.amount);
        FEEDBACK_CONTAINER.html('');
    });

    bean.on(PROMO_CODE_APPLY[0], 'click', () => {
        validateCode(PROMO_CODE.val());
    });

    validateCode(PROMO_CODE.val())
}