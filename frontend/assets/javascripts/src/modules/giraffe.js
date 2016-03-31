define([],function(){
    'use strict';
    var CURRENCY_FIELD = document.QuerySelector('.js-currency-field');

    var CURRENCY_PICKER = document.QuerySelectorAll('.js-currency');

    var ACTIVE = '.active';

    var AMOUNT_PICKER = document.querySelectorAll('[data-amount]');
    var AMOUNT = document.querySelector('.js-amount');

    function init(){
        //Set currency listeners
        setListenerOnChildren('onclick',CURRENCY_PICKER,setCurrency);

        //Set value listeners
        setListenerOnChildren('onclick',AMOUNT_PICKER,setAmount);

        //Set custom value listener
        AMOUNT.addEventListener('blur',clearAmounts);

    }

    //For an element whose children need an event listener
    //and an function of the child which returns a handler:
    function setListenerOnChildren(event,element,handler){
        for (var i = 0; i < element.length; i++) {
            var child = element[i];
            var listener = setCurrency(handler);
            if (handler){
                child.addEventListener(event,listener);
            }
        }

    }

    function setActive(element){
        setInactive(element.parentElement());
        element.classList.add(ACTIVE);
    }
    function setInactive(group){
        group.querySelector(ACTIVE).classList.remove(ACTIVE);
    }

    function setCurrency(element){
        var currency = element.getAttribute('data-currency');
        var symbol =  element.getAttribute('data-symbol');
        if(currency && symbol){
            return function() {
                CURRENCY_FIELD.value = currency;
                for (var i = 0; i < CURRENCY_PICKER.length; i++) {
                    CURRENCY_PICKER[i].innerHTML = symbol;
                }
            }
        }

    }

    function setAmount(e){
        var amount = e.getAttribute('data-amount');
        if(amount){
            return function() {
                AMOUNT.value = amount;
                setActive(e);
            }
        }
    }

    function clearAmounts(){
        clearAmounts(AMOUNT_PICKER);
    }


    return({
        init:init
    })
});


