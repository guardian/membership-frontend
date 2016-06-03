import * as display from 'src/modules/form/validation/display'
import * as helper from 'src/utils/helper'
import * as ajax from 'ajax'
import $ from '$'
import {ophan} from 'src/modules/analytics/ophan';

const ACTIVE_CLASS = 'active';
const AMOUNT_CLASS = 'js-amount';

const CURRENCY_FIELD = document.querySelector('.js-currency-field');
const $CURRENCY_DISPLAY = $('.js-currency');
const $CURRENCY_PICKER = $('.js-currency-switcher');

const $AMOUNT_PICKER = $('[data-amount]');
const CUSTOM_AMOUNT = document.querySelector('.js-amount-field');

const EMAIL_FIELD = document.querySelector('.js-email');
const NAME_FIELD = document.querySelector('.js-name');

const SHOWN_ATTRIBUTE = 'shown';
const SHOWABLE = $('[data-'+SHOWN_ATTRIBUTE+']');
const HIDDEN_CLASS = 'is-hidden';

const $OPHAN = $('.js-ophan-id');


export function init() {
    if (!document.querySelector('.container-global--giraffe .js-form')) {
        return;
    }

    ophanId();

    $CURRENCY_PICKER.each(el => el.addEventListener('click', ev => selectCurrencyElement(ev.currentTarget)));

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
        $('.js-amount-field').val("")

    }));

    // Custom amount
    CUSTOM_AMOUNT.addEventListener('keyup', ev => setAmount(ev.currentTarget.value));
    CUSTOM_AMOUNT.addEventListener('focus', ev => {
        $('.js-button').removeClass(ACTIVE_CLASS);
    });


    var hiddenAmount = $('.js-amount-hidden')[0].value;
    $('.js-pay').html(hiddenAmount);
    if(!hiddenAmount){
        $('.js-currency-pay').addClass(HIDDEN_CLASS);
    }


    getStuffFromIdentity();
}

function select(el) {
    // if we had a real DOM manipulation library (i.e. jQuery) we could do:
    // $(el).closest('.js-button-group').find('.js-button').removeClass(ACTIVE_CLASS);
    $(helper.getSpecifiedParent(el, 'js-button-group').querySelectorAll('.js-button')).removeClass(ACTIVE_CLASS);
    $(el).addClass(ACTIVE_CLASS);
}

function selectCurrencyElement(el) {
    let currency = el.getAttribute('data-currency');
    let symbol = el.getAttribute('data-symbol');
    if (currency && symbol) {
        let shown = $('[data-' + SHOWN_ATTRIBUTE + '*=' + currency + ']'); //Searching the entire DOM because we still don't have a really DOM library
        select(el);
        CURRENCY_FIELD.value = currency;
        $CURRENCY_DISPLAY.html(symbol);
        $(SHOWABLE).addClass(HIDDEN_CLASS);
        shown.removeClass(HIDDEN_CLASS);
    }
}



function setAmount(amount) {
    $('input.' + AMOUNT_CLASS).val(amount);
    $('.' + AMOUNT_CLASS + ':not(input)').html(amount);
    $('.js-currency-pay').addClass(SHOWN_ATTRIBUTE).removeClass(HIDDEN_CLASS);
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

function ophanId(){
    ophan.then(o => {
        $OPHAN.val(o.viewId);
    })
}
