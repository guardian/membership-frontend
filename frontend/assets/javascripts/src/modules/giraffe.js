define(['src/modules/form/validation/display','ajax'], function (display, ajax) {
    'use strict';
    var CURRENCY_FIELD = document.querySelector('.js-currency-field');
    var CURRENCY_DISPLAY = document.querySelectorAll('.js-currency');
    var CURRENCY_PICKER = document.querySelectorAll('.js-currency-switcher');

    var ACTIVE = 'active';

    var AMOUNT_PICKER = document.querySelectorAll('[data-amount]');
    var AMOUNT_DISPLAY = document.querySelectorAll('.js-amount');
    var AMOUNT_FIELD_CLASS = 'js-amount-field';
    var AMOUNT_FIELD = document.querySelector('.' + AMOUNT_FIELD_CLASS);

    var EMAIL_FIELD = document.querySelector('.js-email');
    var NAME_FIELD = document.querySelector('.js-name');


    function init() {
        if (!document.querySelectorAll('.container-global--giraffe').length) {
            return;
        }

        //Set currency listeners
        setListenerOnChildren('click', CURRENCY_PICKER, setCurrency);

        //Set value listeners
        setListenerOnChildren('click', AMOUNT_PICKER, setAmount);

        //Set custom value listener
        AMOUNT_FIELD.addEventListener('blur', setAmount(AMOUNT_FIELD));

        getStuffFromIdentity();

    }

    //For an element whose children need an event listener
    //and an function of the child which returns a handler:
    function setListenerOnChildren(event, element, handler) {
        for (var i = 0; i < element.length; i++) {
            var child = element[i];
            var listener = handler(child);
            if (handler) {
                child.addEventListener(event, listener);
            }
        }

    }

    function setActive(element) {
        element.classList.add(ACTIVE);
    }

    function setInactive(nodeList) {
            for (var i = 0; i < nodeList.length; i++) {
                var element = nodeList[i];
                if (element.classList.contains(ACTIVE)) {
                    element.classList.remove(ACTIVE)
                    return;
                }
            }

    }

    function setCurrency(element) {
        var currency = element.getAttribute('data-currency');
        var symbol = element.getAttribute('data-symbol');
        if (currency && symbol) {
            return function () {
                setInactive(CURRENCY_PICKER);
                CURRENCY_FIELD.value = currency;
                for (var i = 0; i < CURRENCY_DISPLAY.length; i++) {
                    CURRENCY_DISPLAY[i].innerHTML = symbol;
                }
                setActive(element);
            }
        }

    }

    function setAmount(element) {
        return function () {
            var amount;
            clearAmounts();
            if (element.classList.contains(AMOUNT_FIELD_CLASS)) {
                amount = element.value;
            }
            if (element.getAttribute('data-amount')) {
                amount = element.getAttribute('data-amount') + '.00';
                setActive(element);
                //Force a validation pass if we pick a pre-selected amount
                display.toggleErrorState({
                    isValid:true,
                    elem: AMOUNT_FIELD
                });
            }
            if (amount) {
                for (var i = 0; i < AMOUNT_DISPLAY.length; i++) {
                    var amountdisplay = AMOUNT_DISPLAY[i];
                    if (amountdisplay.tagName == 'INPUT') {
                        amountdisplay.value = amount;
                    } else {
                        amountdisplay.innerHTML = amount;
                    }
                }
            }
        }
    }

    function clearAmounts() {
        setInactive(AMOUNT_PICKER);
    }

    function getStuffFromIdentity() {
        var IDENTITY_API = 'https://idapi.theguardian.com/user/me/';
        //var IDENTITY_API = 'https://idapi-code-proxy.thegulocal.com/user/me';
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

    return {
        init: init
    };
});


