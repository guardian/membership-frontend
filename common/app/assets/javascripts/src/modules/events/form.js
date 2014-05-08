define([
    '$',
    'bean',
    'config/appCredentials',
    'src/utils/user',
    'stripe'
], function($, bean, appCredentials, userUtil, stripe){
    'use strict';

    function StripePaymentForm(){}

    StripePaymentForm.prototype.config = {

        class: {
            FORM_FIELD_ERROR: 'form-field--error',
            ERROR_CARD_NUMBER: 'error--card-number',
            ERROR_CARD_CVC: 'error--card-cvc',
            ERROR_CARD_EXPIRY: 'error--card-expiry',
            HIDE: 'hide',
            CREDIT_CARD_EXPIRY_YEAR: 'credit-card-expiry-year',
            PAYMENT_ERRORS: 'payment-errors',
            FORM_SUBMIT: 'submit-input'
        },
        id: {
            CREDIT_CARD_NUMBER: 'credit-card-number',
            CREDIT_CARD_CVC: 'credit-card-cvc',
            CREDIT_CARD_EXPIRY_MONTH: 'credit-card-expiry-month',
            PAYMENT_FORM: 'payment-form'
        },
        domElements: {}
    };

    StripePaymentForm.prototype.domElementSetup = function(){

        var config = this.config;
        var classNames = config.class;
        var ids = config.id;

        for(var className in classNames){

            var camelCasedName = this.camelCase(className);

            config.domElements[camelCasedName] = this.getElementByClass(classNames[className]);
        }

        for(var id in ids){

            var camelCasedName = this.camelCase(id);

            config.domElements[camelCasedName] = this.getElementById(ids[id]);
        }

    };

    StripePaymentForm.prototype.camelCase = function(string){

        var camelCased = string.toLowerCase().split('_').map(function(str, i){

            if(0 === i ){
                return str;
            }

            return str[0].toUpperCase() + str.slice(1);
        }).join('');

        return camelCased;
    };

    StripePaymentForm.prototype.getElementByClass = function(className){
        return $('.' + className);
    };

    StripePaymentForm.prototype.getElementById = function(id){
        return $('#' + id);
    };

    StripePaymentForm.prototype.getElement = function(element){
        return this.config.domElements[element];
    };

    StripePaymentForm.prototype.stripeResponseHandler = function (status, response) {

        var $paymentErrors = this.getElement('paymentErrors');
        var $submitButton = this.getElement('formSubmit');
        var $formElement = this.getElement('paymentForm');

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

    StripePaymentForm.prototype.populateUserInformation = function () {

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

    StripePaymentForm.prototype.addListeners = function(){

        var $creditCardNumberElement = this.getElement('creditCardNumber');
        var $creditCardCVCElement = this.getElement('creditCardCvc');
        var $creditCardExpiryMonthElement = this.getElement('creditCardExpiryMonth');
        var $creditCardExpiryYearElement = this.getElement('creditCardExpiryYear');
        var $formElement = this.getElement('paymentForm');


        bean.on($creditCardNumberElement[0], 'blur', function(){
            this.manageValidationResult(this.validateCardNumber());
        }.bind(this));

        bean.on($creditCardCVCElement[0], 'blur', function(){
            this.manageValidationResult(this.validateCVC());
        }.bind(this));

        bean.on($creditCardExpiryMonthElement[0] , 'blur', function(){
            this.manageValidationResult(this.validateExpiry());
        }.bind(this));

        bean.on($creditCardExpiryYearElement[0] , 'blur', function(){
            this.manageValidationResult(this.validateExpiry());
        }.bind(this));


        bean.on($formElement[0], 'submit', function(e){

            var formValidationResult;

            e.preventDefault();

            formValidationResult = this.isFormValid();

            if(formValidationResult.isValid){

                stripe.card.createToken({
                    number: $creditCardNumberElement.val(),
                    cvc: $creditCardCVCElement.val(),
                    exp_month: $creditCardExpiryMonthElement.val(),
                    exp_year: $creditCardExpiryYearElement.val()
                }, this.stripeResponseHandler);

            } else{

                formValidationResult.errors.forEach(function(validationResult){

                    this.manageValidationResult(validationResult);
                }, this);
            }
        }.bind(this));
    };

    StripePaymentForm.prototype.validateCardNumber = function(){

        var $creditCardNumberElement = this.getElement('creditCardNumber');
        var isValid = stripe.card.validateCardNumber($creditCardNumberElement.val());

        return {
            isValid: isValid,
            errorElementClassName: this.config.class.ERROR_CARD_NUMBER
        };
    };

    StripePaymentForm.prototype.validateCVC = function(){

        var $creditCardCVCElement = this.getElement('creditCardCvc');
        var isValid = stripe.card.validateCVC($creditCardCVCElement.val());

        return {
            isValid: isValid,
            errorElementClassName: this.config.class.ERROR_CARD_CVC
        };
    };

    StripePaymentForm.prototype.validateExpiry = function(){

        var $creditCardExpiryMonthElement = this.getElement('creditCardExpiryMonth');
        var $creditCardExpiryYearElement = this.getElement('creditCardExpiryYear');
        var isValid = stripe.card.validateExpiry($creditCardExpiryMonthElement.val(), $creditCardExpiryYearElement.val());

        return {
            isValid: isValid,
            errorElementClassName: this.config.class.ERROR_CARD_EXPIRY
        };
    };

    StripePaymentForm.prototype.isFormValid = function(){

        var errors = [];
        var validationProfiles = [
            this.validateCardNumber,
            this.validateCVC,
            this.validateExpiry
        ];

        for(var profile in validationProfiles){

            var validationProfile = validationProfiles[profile];

            if(validationProfiles.hasOwnProperty(profile) && 'function' === typeof validationProfile){

                var result = validationProfile.call(this);

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

    StripePaymentForm.prototype.manageValidationResult = function(validationResult){

        var $errorMessageElement = $('.' + validationResult.errorElementClassName);
        var $parentRow = $errorMessageElement.parent();
        var configClass = this.config.class;

        $parentRow.removeClass(configClass.FORM_FIELD_ERROR);

        if (!validationResult.isValid) {

            $parentRow.addClass(configClass.FORM_FIELD_ERROR);

            if ($errorMessageElement.hasClass(configClass.HIDE)) {
                $errorMessageElement.toggleClass(configClass.HIDE);
            }

        } else {

            if (!$errorMessageElement.hasClass(configClass.HIDE)) {
                $errorMessageElement.addClass(configClass.HIDE);
            }
        }
    };

    StripePaymentForm.prototype.init = function () {

        this.domElementSetup();

        this.populateUserInformation();

        this.addListeners();

        stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
    };

    return StripePaymentForm;
});