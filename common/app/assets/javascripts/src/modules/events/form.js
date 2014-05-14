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
        classes: {
            STRIPE_FORM: 'js-stripe-form',
            FORM_FIELD_ERROR: 'js-form-field--error',
            ERROR_CARD_NUMBER: 'js-error--card-number',
            ERROR_CARD_CVC: 'js-error--card-cvc',
            ERROR_CARD_EXPIRY: 'js-error--card-expiry',
            HIDE: 'js-hide',
            PAYMENT_ERRORS: 'js-payment-errors',
            FORM_SUBMIT: 'js-submit-input',
            CREDIT_CARD_NUMBER: 'js-credit-card-number',
            CREDIT_CARD_CVC: 'js-credit-card-cvc',
            CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-expiry-month',
            CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-expiry-year'
        },
        DOM: {}
    };

    StripePaymentForm.prototype.domElementSetup = function(){
        for(var className in this.config.classes){
            this.config.DOM[className] = this.context.querySelector('.'+this.config.classes[className]);
        }
    };

    StripePaymentForm.prototype.getElement = function(element){
        return $(this.config.DOM[element]);
    };

    StripePaymentForm.prototype.stripeResponseHandler = function (status, response) {

        var $paymentErrors = this.getElement('PAYMENT_ERRORS'),
            $submitButton = this.getElement('FORM_SUBMIT'),
            $formElement = $(this.context);

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

        var $creditCardNumberElement = this.getElement('CREDIT_CARD_NUMBER'),
            $creditCardCVCElement = this.getElement('CREDIT_CARD_CVC'),
            $creditCardExpiryMonthElement = this.getElement('CREDIT_CARD_EXPIRY_MONTH'),
            $creditCardExpiryYearElement = this.getElement('CREDIT_CARD_EXPIRY_YEAR'),
            $formElement = $(this.context);


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
            errorElementClassName: this.config.classes.ERROR_CARD_NUMBER
        };
    };

    StripePaymentForm.prototype.validateCVC = function(){

        var $creditCardCVCElement = this.getElement('creditCardCvc');
        var isValid = stripe.card.validateCVC($creditCardCVCElement.val());

        return {
            isValid: isValid,
            errorElementClassName: this.config.classes.ERROR_CARD_CVC
        };
    };

    StripePaymentForm.prototype.validateExpiry = function(){

        var $creditCardExpiryMonthElement = this.getElement('creditCardExpiryMonth');
        var $creditCardExpiryYearElement = this.getElement('creditCardExpiryYear');
        var isValid = stripe.card.validateExpiry($creditCardExpiryMonthElement.val(), $creditCardExpiryYearElement.val());

        return {
            isValid: isValid,
            errorElementClassName: this.config.classes.ERROR_CARD_EXPIRY
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
        var configClasses = this.config.classes;

        $parentRow.removeClass(configClasses.FORM_FIELD_ERROR);

        if (!validationResult.isValid) {

            $parentRow.addClass(configClasses.FORM_FIELD_ERROR);

            if ($errorMessageElement.hasClass(configClasses.HIDE)) {
                $errorMessageElement.toggleClass(configClasses.HIDE);
            }

        } else {

            if (!$errorMessageElement.hasClass(configClasses.HIDE)) {
                $errorMessageElement.addClass(configClasses.HIDE);
            }
        }
    };

    StripePaymentForm.prototype.init = function (context) {

        //TODO init guard/element on page detection needs a thought here

        this.context = context || document.querySelector('.'+this.config.classes.STRIPE_FORM);

        if (this.context) {
            this.domElementSetup();

            this.populateUserInformation();

            this.addListeners();

            stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
        }
    };

    return StripePaymentForm;
});