define([], function () {
    'use strict';
    var CURRENCY_FIELD = document.querySelector('.js-currency-field');
    var CURRENCY_DISPLAY = document.querySelectorAll('.js-currency');
    var CURRENCY_PICKER = document.querySelectorAll('.js-currency-switcher');

    var ACTIVE = 'active';

    var AMOUNT_PICKER = document.querySelectorAll('[data-amount]');
    var AMOUNT_DISPLAY = document.querySelectorAll('.js-amount');
    var AMOUNT_FIELD_CLASS = 'js-amount-field';
    var AMOUNT_FIELD = document.querySelector('.' + AMOUNT_FIELD_CLASS);
    console.log('hello');

    function init() {
        console.log('hello');
        //Set currency listeners
        setListenerOnChildren('click', CURRENCY_PICKER, setCurrency);

        //Set value listeners
        setListenerOnChildren('click', AMOUNT_PICKER, setAmount);

        //Set custom value listener
        AMOUNT_FIELD.addEventListener('blur', setAmount(AMOUNT_FIELD));

    }

    //For an element whose children need an event listener
    //and an function of the child which returns a handler:
    function setListenerOnChildren(event, element, handler) {
        for (var i = 0; i < element.length; i++) {
            var child = element[i];
            var listener = handler(child);
            if (handler) {
                console.log('Adding', event, listener);
                child.addEventListener(event, listener);
            }
        }

    }

    function setActive(element) {
        console.log('1');
        console.log(element);
        setInactive(element.parentElement.childNodes);
        element.classList.add(ACTIVE);
    }

    function setInactive(nodeList) {
        if (nodeList instanceof NodeList) {
            for (var i = 0; i < nodeList.length; i++) {
                var element = nodeList[i];
                if (element.classList.contains(ACTIVE)) {
                    element.classList.remove(ACTIVE)
                    return;
                }
            }
        }
    }

    function setCurrency(element) {
        console.log('set e');
        console.log(element);
        var currency = element.getAttribute('data-currency');
        var symbol = element.getAttribute('data-symbol');
        console.log(currency, symbol);
        if (currency && symbol) {
            return function () {
                console.log('HELLO');
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
                console.log('field')
            }
            if (element.getAttribute('data-amount')) {
                amount = element.getAttribute('data-amount');
                setActive(element);
                console.log('select');
            }
            console.log(amount)
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


    return {
        init: init
    };
});


