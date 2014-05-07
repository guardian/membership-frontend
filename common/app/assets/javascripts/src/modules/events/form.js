define([
    '$',
    'bean',
    'config/appCredentials',
    'src/utils/user',
    'stripe'
], function($, bean, appCredentials, userUtil, stripe){
    'use strict';

    var config = {
        classes: {
            FORM_FIELD_ERROR: 'form-field--error',
            ERROR_CARD_NUMBER: 'error--card-number',
            ERROR_CARD_CVC: 'error--card-cvc',
            ERROR_CARD_EXPIRY: 'error--card-expiry',
            HIDE: 'hide'
        }
    };

    var $creditCardNumberElement = $('.cc-num');
    var $creditCardCVCElement = $('.cc-cvc');
    var $creditCardExpiryMonthElement = $('.cc-exp-month');
    var $creditCardExpiryYearElement = $('.cc-exp-year');
    var $formElement = $('#payment-form');
    var $paymentErrors = $('.payment-errors');
    var $submitButton = $('.submit-input');

    var stripeResponseHandler = function (status, response) {

        if (response.error) {
            $paymentErrors.text(response.error.message);
            $submitButton.attr('disabled', false);
        } else {

            // token contains id, last4, and card type
            var token = response.id;
            // Insert the token into the form so it gets submitted to the server

            var hiddenInput = $.create('<input type="hidden" name="stripeToken" />').val(token);

            $formElement.append(hiddenInput);
            // and submit
            $formElement[0].submit();
        }
    };

    var populateUserInformation = function () {

        var user = userUtil.getUserFromCookie();
        var userElement = $('.user');

        if (user) {

            userElement.html([
                'User from cookie - ',
                'displayname: ' + user.displayname,
                'id: ' + user.id,
                'email: ' + user.primaryemailaddress
            ].join('<br/>'));
        }
    };

    var addListeners = function(){

        bean.on($creditCardNumberElement[0], 'blur', function(){

            var validationResult = validation.validateCardNumber();

            manageValidationResult(validationResult);
        });

        bean.on($creditCardCVCElement[0], 'blur', function(){

            var validationResult = validation.validateCVC();

            manageValidationResult(validationResult);
        });

        bean.on($creditCardExpiryMonthElement[0] , 'blur', function(){

            var validationResult = validation.validateExpiry();

            manageValidationResult(validationResult);
        });

        bean.on($creditCardExpiryYearElement[0] , 'blur', function(){

            var validationResult = validation.validateExpiry();

            manageValidationResult(validationResult);
        });


        bean.on($formElement[0], 'submit', function(e){

            var formValidationResult;

            e.preventDefault();

            formValidationResult = isFormValid();

            if(formValidationResult.isValid){

                stripe.card.createToken({
                    number: $creditCardNumberElement.val(),
                    cvc: $creditCardCVCElement.val(),
                    exp_month: $creditCardExpiryMonthElement.val(),
                    exp_year: $creditCardExpiryYearElement.val()
                }, stripeResponseHandler);

            } else{

                formValidationResult.errors.forEach(function(validationResult){

                    manageValidationResult(validationResult);
                });
            }
        });
    };

    var validation = {

        validateCardNumber: function(){

            var isValid = stripe.card.validateCardNumber($creditCardNumberElement.val());

            return {
                isValid: isValid,
                errorElementClassName: config.classes.ERROR_CARD_NUMBER
            };
        },

        validateCVC: function(){

            var isValid = stripe.card.validateCVC($creditCardCVCElement.val());

            return {
                isValid: isValid,
                errorElementClassName: config.classes.ERROR_CARD_CVC
            };
        },

        validateExpiry: function(){

            var isValid = stripe.card.validateExpiry($creditCardExpiryMonthElement.val(), $creditCardExpiryYearElement.val());

            return {
                isValid: isValid,
                errorElementClassName: config.classes.ERROR_CARD_EXPIRY
            };
        }
    };


    var isFormValid = function(){

        var errors = [];

        for(var profile in validation){

            var validationProfile = validation[profile];

            if(validation.hasOwnProperty(profile) && 'function' === typeof validationProfile){

                var result = validationProfile();

                if(!result.isValid){
                    errors.push(result);
                }
            }
        }

        return {
            isValid: !errors.length,
            errors: errors
        };
    };

    var manageValidationResult = function(validationResult){

        var $errorMessageElement = $('.' + validationResult.errorElementClassName);
        var $parentRow = $errorMessageElement.parent();

        $parentRow.removeClass(config.classes.FORM_FIELD_ERROR);

        if (!validationResult.isValid) {

            $parentRow.addClass(config.classes.FORM_FIELD_ERROR);

            if ($errorMessageElement.hasClass(config.classes.HIDE)) {
                $errorMessageElement.toggleClass(config.classes.HIDE);
            }

        } else {

            if (!$errorMessageElement.hasClass(config.classes.HIDE)) {
                $errorMessageElement.addClass(config.classes.HIDE);
            }
        }
    };

    var init = function () {

        populateUserInformation();

        addListeners();

        stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
    };

    return {
        init: init
    };

});