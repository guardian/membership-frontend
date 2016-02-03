define(['ajax'],function(ajax){
    'use strict';
    //GET PROMO CODE FIELD
    var inputBox = document.getElementById('promo-code'),
       countrySelect = document.getElementsByClassName('select js-country'),
       lookupUrl = inputBox.data('lookup-url'),
        submitted = false;
    countrySelect;
    lookupUrl;
    ajax;
/*
    var submit = function(){
       submitted = true;
       validatePromoCode();
    }
  */

    var validatePromoCode = function(){
        //Check whether a promo code has been entered- return early.
        if (!submitted) {
            return true;
        }
/*
        ajax({
            type: 'json',
            method: 'GET',
            url: lookupUrl,
            data: {
                promoCode: promoCode,
                //billing period
                //tier
                country: countrySelect.options[countrySelect.selectedIndex]
            }
        }).then(function (a) {
            if (a.isValid) {
                displayPromotion(a.promotion);
                bindExtraKeyListener();
            } else {
                displayError(a.errorMessage);
            }
        }).catch(function (a) {
            // Content of error codes are not parsed by ajax/reqwest.
            if (a && a.response) {
                var b = JSON.parse(a.response);
                if (b && b.errorMessage) {
                    displayError(b.errorMessage);
                    return;
                }
            }
            displayError();
        });
*/
        return true;
    };





    /**
     * useful things:
     *  $
     *  ajax!
     *    ajax({
            type: 'json',
            method: 'GET',
            url: lookupUrl,
            data: {
                promoCode: promoCode,
                productRatePlanId: formElements.getRatePlanId(),
                country: countryChoice.getCurrentCountryOption().value
            }
        }).then(function (a) {
            if (a.isValid) {
                displayPromotion(a.promotion);
                bindExtraKeyListener();
            } else {
                displayError(a.errorMessage);
            }
        }).catch(function (a) {
            // Content of error codes are not parsed by ajax/reqwest.
            if (a && a.response) {
                var b = JSON.parse(a.response);
                if (b && b.errorMessage) {
                    displayError(b.errorMessage);
                    return;
                }
            }
            displayError();
        });
     */

    return{
        validatePromoCode:validatePromoCode
    }
});
