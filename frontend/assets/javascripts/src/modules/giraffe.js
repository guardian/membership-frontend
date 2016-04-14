import * as display from 'src/modules/form/validation/display'
import * as helper from 'src/utils/helper'
import * as ajax from 'ajax'
import * as $ from '$'

const ACTIVE_CLASS = 'active';
const AMOUNT_CLASS = 'js-amount';

const CURRENCY_FIELD = document.querySelector('.js-currency-field');
const $CURRENCY_DISPLAY = $('.js-currency');
const $CURRENCY_PICKER = $('.js-currency-switcher');

const $AMOUNT_PICKER = $('[data-amount]');
const CUSTOM_AMOUNT = document.querySelector('.js-amount-field');

const EMAIL_FIELD = document.querySelector('.js-email');
const NAME_FIELD = document.querySelector('.js-name');

export function init() {
    if (!document.querySelector('.container-global--giraffe .js-form')) {
        return;
    }

    $CURRENCY_PICKER.each(el => el.addEventListener('click', ev => setCurrency(ev.currentTarget)));

    // Preset amount
    $AMOUNT_PICKER.each(el => el.addEventListener('click', ev => {
        let element = ev.currentTarget;
        let amount = element.getAttribute('data-amount') + '.00';

        select(element);

        // Force a validation pass if we pick a pre-selected amount
        display.toggleErrorState({
            isValid: true,
            elem: CUSTOM_AMOUNT
        });
        setAmount(amount);
    }));

    // Custom amount
    CUSTOM_AMOUNT.addEventListener('blur', ev => setAmount(ev.currentTarget.value));

    getStuffFromIdentity();
}

function select(el) {
    // if we had a real DOM manipulation library (i.e. jQuery) we could do:
    // $(el).closest('.js-button-group').find('.js-button').removeClass(ACTIVE_CLASS);
    $(helper.getSpecifiedParent(el, 'js-button-group').querySelectorAll('.js-button')).removeClass(ACTIVE_CLASS);
    $(el).addClass(ACTIVE_CLASS);
}

function setCurrency(el) {
    let currency = el.getAttribute('data-currency');
    let symbol = el.getAttribute('data-symbol');
    if (currency && symbol) {
        select(el);
        CURRENCY_FIELD.value = currency;
        $CURRENCY_DISPLAY.html(symbol);
    }
}

function setAmount(amount) {
    $('input.' + AMOUNT_CLASS).val(amount);
    $('.' + AMOUNT_CLASS + ':not(input)').html(amount);
}

function getStuffFromIdentity() {
    let IDENTITY_API = 'https://idapi.theguardian.com/user/me/';
    ajax.reqwest({
        url: IDENTITY_API,
        method: 'get',
        type: 'jsonp',
        crossOrigin: true
    }).then(function (resp) {
        if (resp.user) {
            EMAIL_FIELD.value = resp.user.primaryEmailAddress;
            NAME_FIELD.value = resp.user.publicFields.displayName;
        }
    })
}
