/*global Raven */
define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/user',
    'src/utils/masker',
    'stripe',
    'ajax',
    'config/stripeErrorMessages',
    'src/utils/helper'
], function ($, bean, component, userUtil, masker, stripe, ajax, stripeErrorMessages, utilsHelper) {
    'use strict';

    /**
     * This class has grown somewhat, I am happy with the majority of it but parts of it need a bit of a
     * rethink as a few things have been attached for the moment I am leaving it because although verbose it works.
     * TODO-ben: simplify class
     * TODO-ben: move errors to above inputs (confirm with UX and Design)
     */
    var MAKING_PAYMENT_MESSAGE = 'Making Payment...';
    var PROCESSING_MESSAGE = 'Processing';
    var PAYMENT_MESSAGE = 'Payment';
    var ELLIPSE = '...';
    var GLOBAL_FORM_MESSAGE = 'This form has errors';
    var self;

    function Form (formElement) {
        self = this;
        this.formElement = formElement;
        this.errorMessages = [];
        this.validationProfiles = [];
    }

    component.define(Form);

    Form.prototype.classes = {
        HIDE: 'is-hidden',
        PAYMENT_ERRORS: 'js-payment-errors',
        FORM_FIELD_ERROR_MESSAGE: 'form-field__error-message',
        FORM_SUBMIT: 'js-submit-input',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        CREDIT_CARD_IMAGE: 'js-credit-card-image',
        THROBBER: 'js-waiting-container',
        THROBBER_MESSAGE: 'js-waiting-message',
        ICON_PREFIX: 'icon-sprite-card-'
    };

    Form.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Form.prototype.displayMonthError = false;
    Form.prototype.validator = {};
    Form.prototype.listener = {};
    Form.prototype.isStripeForm = false;

    /**
     * build the data object to be sent to the server
     * @param form
     * @param mixin
     * @returns {{} || undefined }
     */
    Form.prototype.buildDataObject = function (form, mixin) {

        var dataObj = {},
            formElements;

        if (mixin){
            for (var prop in mixin) {
                if (mixin.hasOwnProperty(prop)) {
                    dataObj[prop] = mixin[prop];
                }
            }
        }

        if (!form || form.nodeName !== 'FORM') {
            return;
        }

        formElements = Array.prototype.slice.call(form.elements);

        for (var i = 0, formElementLength = formElements.length; i < formElementLength; i++) {

            var element = formElements[i];

            if (element.name !== '' && element.type && (element.type !== 'checkbox' && element.type !== 'radio' || element.checked)) {
                dataObj[element.name] = element.value;
            }
        }

        return dataObj;
    };

    /**
     * handle response from stripe, success or error failure
     * @param status
     * @param response
     */
    Form.prototype.stripeResponseHandler = function (status, response) {

        if (response.error) {
            var errorMessage = self.getErrorMessage(response.error);
            if (errorMessage) {
                self.manageErrors({
                    isValid: false,
                    errorMessage: errorMessage,
                    $element: $(self.getClass('CREDIT_CARD_NUMBER'), self.formElement)
                });
                self.stopLoader();
                self.setThrobberMessage();
            }
        } else {

            var token = response.id;
            var data = self.buildDataObject(self.formElement, {
                'payment.token': token
            });

            self.setThrobberMessage(MAKING_PAYMENT_MESSAGE);

            ajax({
                url: self.formElement.action,
                method: 'post',
                data: data,
                success: function (data) {
                    window.location.assign(data.redirect);
                },
                error: function (error) {
                    var errorObj,
                        errorMessage;

                    try {
                        errorObj = error.response && JSON.parse(error.response);
                        errorMessage = self.getErrorMessage(errorObj);
                        if (errorMessage) {
                            self.manageErrors({
                                isValid: false,
                                errorMessage: errorMessage,
                                $element: $(self.getClass('CREDIT_CARD_NUMBER'), self.formElement)
                            });
                        }
                    } catch (e) {
                        Raven.captureException(e);
                    }

                    self.stopLoader();
                    self.setThrobberMessage();
                }
            });

        }
    };

    /**
     * get friendly error messages via codes sent from stripe
     * @param errorObj {type: 'card_error', code: 'incorrect_number', decline_code: 'do_not_honour'}
     * @returns {*}
     */
    Form.prototype.getErrorMessage = function (errorObj) {

        var errorCode = errorObj && errorObj.code,
            errorType = errorObj && errorObj.type,
            errorSection = stripeErrorMessages[errorType],
            errorMessage;

        if (errorSection) {
            errorMessage = errorSection[errorCode];

            if (errorCode === 'card_declined') {
                errorMessage = errorSection.card_declined[errorObj.decline_code];
                if (!errorMessage) {
                    errorMessage = errorSection.card_declined.generic_decline;
                }
            }
        }

        if (!errorMessage) {
            errorMessage = stripeErrorMessages.generic_error;
        }

        return errorMessage;
    };

    /**
     * hide or show error messages in the error element at the top of the page,
     * disable or enable the submit button dependant on if we have errors
     * @param errorMessages
     */
    Form.prototype.displayGlobalErrors = function (errorMessages) {
        var $paymentErrorsElement = $(this.getClass('PAYMENT_ERRORS'), this.formElement);
        var errorString = '';

        if (errorMessages.length) {
            //display errors and disable submit
            errorString += '<li>' + GLOBAL_FORM_MESSAGE + '</li>';

            $paymentErrorsElement.removeClass(this.classes.HIDE);
            $paymentErrorsElement.html(errorString);

        } else {
            //hide errors and enable submit
            $paymentErrorsElement.html('');
            $paymentErrorsElement.addClass(this.classes.HIDE);
        }
    };

    /**
     * get the card type from strip method
     * @param cardNumber
     * @returns {string}
     */
    Form.prototype.getCardType = function (cardNumber) {
        return stripe.cardType(cardNumber).toLowerCase().replace(' ', '-');
    };

    /**
     * display the relevant card type
     * @param creditCardNumber
     */
    Form.prototype.displayCardTypeImage = function (creditCardNumber) {
        var cardType = this.getCardType(creditCardNumber),
            $creditCardImageElement = $(this.getClass('CREDIT_CARD_IMAGE'), this.formElement);

        var re = new RegExp('\\b' + this.classes.ICON_PREFIX + '\\S*', 'gi');
        $creditCardImageElement[0].className = $creditCardImageElement[0].className.replace(re, '');
        $creditCardImageElement.addClass(this.classes.ICON_PREFIX + cardType);
    };

    /**
     * setup validation for the form,
     * this will add specific validator events to specified elements,
     * it will throw an error if an incorrect validation event method has been supplied
     * @param validationDetail
     * @returns {Form}
     */
    Form.prototype.setupValidation = function (validationDetail) {
        var args;
        var validator;

        for (var i = 0, validationDetailLength = validationDetail.length; i < validationDetailLength; i++) {
            var elem;

            validator = validationDetail[i];
            elem = validator.elem instanceof Array ? validator.elem[0] : validator.elem;

            this.checkValidatorExistsException(validator.name);

            $('.label', utilsHelper.getSpecifiedParent($(elem), 'form-field')).addClass('required-marker');

            args = self.createArgsArray(validator.elem);
            self[validator.name].apply(self, args);
        }

        return this;
    };

    Form.prototype.addValidation = function ($elements) {
        $elements.forEach(function ($element) {
            $element.attr('data-validation', 'required');
        });

        this.setupFormValidation();
    };

    Form.prototype.removeValidation = function ($elements) {
        var errorMessages = [];

        $elements.forEach(function ($element) {
            var $formField = utilsHelper.getSpecifiedParent($element, 'form-field');

            errorMessages.push($element.attr('data-error-message'));
            $element.removeAttr('data-validation');
            $formField.removeClass('form-field--error');
            $('.label', $formField).removeClass('required-marker');
            $('.form-field__error-message', $formField).remove();
        });

        if (errorMessages.length) {
            this.flushErrors(errorMessages);
        }

        this.setupFormValidation();
    };

    Form.prototype.checkValidatorExistsException = function (validatorName) {
        if (!self[validatorName] && typeof self[validatorName] !== 'function') {
            throw 'please specify an existing validation profile';
        }
    };


    /**
     * create arguments array to be used with apply function
     * @param elem
     * @returns {*}
     */
    Form.prototype.createArgsArray = function (elem, args) {

        var argsArray;
        argsArray = elem instanceof Array ? elem : [elem];

        if (args) {

            argsArray = argsArray.concat(args);
        }

        return argsArray;
    };

    /**
     * validation event listener for required inputs
     * @param element
     */
    Form.prototype.required = function (element) {
        bean.on(element, 'blur', function (e) {
            var validationResult = self.requiredValidator(e.target);
            self.manageErrors(validationResult);
        });

        self.validationProfiles.push({
            elem: element,
            validator: 'requiredValidator'
        });
    };

    /**
     * validation event listener for error messages on input lengths
     * @param element
     */
    Form.prototype.length = function (element) {
        var args = JSON.parse(element.getAttribute('data-arguments'));
        args.unshift(element);

        bean.on(element, 'blur', function () {
            var validationResult = self.lengthValidator.apply(this, args);
            self.manageErrors(validationResult);
        });

        self.validationProfiles.push({
            elem: element,
            args: element.getAttribute('data-arguments'),
            validator: 'lengthValidator'
        });
    };

    /**
     * validation event listener for credit card element
     * @param creditCardNumberElement
     */
    Form.prototype.creditCardNumber = function (creditCardNumberElement) {

        self.isStripeForm = true;

        bean.on(creditCardNumberElement, 'blur', function (e) {
            var validationResult = self.creditCardNumberValidator.call(self, e.target);
            self.manageErrors(validationResult);
        });

        bean.on(creditCardNumberElement, 'keyup blur', masker(' ', 4));

        bean.on(creditCardNumberElement, 'keyup blur', function (e) {
            self.displayCardTypeImage($(e.target).val());
        });

        self.validationProfiles.push({
            elem: creditCardNumberElement,
            validator: 'creditCardNumberValidator'
        });
    };

    /**
     * validation event listener for credit card CVC
     * @param creditCardCVCElement
     */
    Form.prototype.creditCardCVC = function (creditCardCVCElement) {
        bean.on(creditCardCVCElement, 'blur', function (e) {
            var validationResult = self.creditCardCVCValidator(e.target);
            self.manageErrors(validationResult);
        });

        self.validationProfiles.push({
            elem: creditCardCVCElement,
            validator: 'creditCardCVCValidator'
        });
    };

    /**
     * validation event listner for credit card expiry
     * @param creditCardExpiryMonthElement
     * @param creditCardExpiryYearElement
     */
    Form.prototype.creditCardExpiry = function (creditCardExpiryMonthElement, creditCardExpiryYearElement) {
        bean.on(creditCardExpiryMonthElement, 'change blur', function () {
            var validationResult;

            self.setDisplayMonthError(creditCardExpiryYearElement);
            validationResult = self.creditCardExpiryDateValidator(creditCardExpiryMonthElement, creditCardExpiryYearElement);

            self.manageErrors(validationResult);
        });

        bean.on(creditCardExpiryYearElement, 'change blur', function () {
            var validationResult;

            self.displayMonthError = true;
            validationResult = self.creditCardExpiryDateValidator(creditCardExpiryMonthElement, creditCardExpiryYearElement);

            self.manageErrors(validationResult);
        });

        self.validationProfiles.push({
            elem: [creditCardExpiryMonthElement, creditCardExpiryYearElement],
            validator: 'creditCardExpiryDateValidator'
        });
    };

    /**
     * validation event listner for submit button
     */
    Form.prototype.submitButton = function () {

        bean.on(this.formElement, 'submit', function (e) {
            e.preventDefault();

            // turn month select errors on when submitting
            self.displayMonthError = true;

            var formValidationResult = self.isFormValid();
            var validationResult;

            if (formValidationResult.isValid) {

                var throbberMessage = self.isStripeForm
                    ? [PROCESSING_MESSAGE , ' ', PAYMENT_MESSAGE, ELLIPSE].join('')
                    : PROCESSING_MESSAGE + ELLIPSE;

                self.startLoader();
                self.setThrobberMessage(throbberMessage);

                if( self.isStripeForm ){
                    stripe.card.createToken({
                        number: $(self.getClass('CREDIT_CARD_NUMBER'), self.formElement).val(),
                        cvc: $(self.getClass('CREDIT_CARD_CVC'), self.formElement).val(),
                        exp_month: $(self.getClass('CREDIT_CARD_EXPIRY_MONTH'), self.formElement).val(),
                        exp_year: $(self.getClass('CREDIT_CARD_EXPIRY_YEAR'), self.formElement).val()
                    }, self.stripeResponseHandler);
                } else {
                    self.formElement.submit();
                }

            } else {
                for (var i = 0, formErrorsLength = formValidationResult.errors.length; i < formErrorsLength; i++) {
                    validationResult = formValidationResult.errors[i];
                    self.manageErrors(validationResult);
                }
            }
        });
    };

    /**
     * flush errors from the errorMessages array. This is used when we wish to remove specific errors from
     * the error display
     * @param formErrors
     */
    Form.prototype.flushErrors = function (formErrors) {

        var formError;
        var removeErrorMessage = function (errorMessage, i, errorMessages) {
            if (errorMessage === formError) {
                errorMessages.splice(i, 1);
            }
        };

        if (this.errorMessages.length) {

            for(var i = 0, formErrorsLength = formErrors.length; i < formErrorsLength; i++) {
                formError = formErrors[i];
                this.errorMessages.map(removeErrorMessage);
            }

            this.displayGlobalErrors(this.errorMessages);
        }
    };

    /**
     * validator for required event
     * @param element
     * @returns {{isValid: boolean, errorMessage: *, $element: (*|Bonzo|HTMLElement)}}
     */
    Form.prototype.requiredValidator = function (element) {
        var $element = $(element);
        return {
            isValid: !!$element.val(),
            errorMessage: $element.attr('data-error-message'),
            $element: $element
        };
    };

    /**
     * validator for input lengths
     * @param element
     * @returns {{isValid: boolean, errorMessage: *, $element: (*|Bonzo|HTMLElement)}}
     */
    Form.prototype.lengthValidator = function (element, minLength, maxLength) {
        var $element = $(element);
        return {
            isValid: $element.val().length >= minLength && $element.val().length <= maxLength,
            errorMessage: $element.attr('data-error-message'),
            $element: $element
        };
    };

    /**
     * validator for credit card event
     * @param creditCardNumberElement
     * @returns {{isValid: ({isValid: *, errorMessage: (config|stripeErrorMessages.card_error.incorrect_number|*), $element: *}|*), errorMessage: (config/stripeErrorMessages.card_error.incorrect_number|*), $element: (*|Bonzo|HTMLElement)}}
     */
    Form.prototype.creditCardNumberValidator = function (creditCardNumberElement) {
        var $creditCardNumberElement = $(creditCardNumberElement);
        return {
            isValid: stripe.card.validateCardNumber($creditCardNumberElement.val()),
            errorMessage: stripeErrorMessages.card_error.incorrect_number,
            $element: $creditCardNumberElement
        };
    };

    /**
     * validator for cvc event
     * @param creditCardCVCElement
     * @returns {{isValid: ({isValid: *, errorMessage: (config|stripeErrorMessages.card_error.incorrect_cvc|*), $element: *}|*), errorMessage: (config/stripeErrorMessages.card_error.incorrect_cvc|*), $element: (*|Bonzo|HTMLElement)}}
     */
    Form.prototype.creditCardCVCValidator = function (creditCardCVCElement) {
        var $creditCardCVCElement = $(creditCardCVCElement);
        return {
            isValid: stripe.card.validateCVC($creditCardCVCElement.val()),
            errorMessage: stripeErrorMessages.card_error.incorrect_cvc,
            $element: $creditCardCVCElement
        };
    };

    /**
     * validator for required expiry event
     * @param creditCardExpiryMonthElement
     * @param creditCardExpiryYearElement
     * @returns {{isValid: boolean, errorMessage: (config/stripeErrorMessages.card_error.invalid_expiry|*), $element: (*|Bonzo|HTMLElement)}}
     */
    Form.prototype.creditCardExpiryDateValidator = function (creditCardExpiryMonthElement, creditCardExpiryYearElement) {

        var $creditCardExpiryMonthElement = $(creditCardExpiryMonthElement),
            $creditCardExpiryYearElement = $(creditCardExpiryYearElement);

        var today = new Date(),
            isValid = !self.displayMonthError,
            validDateEntry = function () {
                var presentOrFutureMonth = true,
                    monthAndYearHaveValue = $creditCardExpiryMonthElement[0].selectedIndex > 0 &&
                        $creditCardExpiryYearElement[0].selectedIndex > 0;

                // if we are on the current year check the month is the current or a future month
                if ($creditCardExpiryYearElement.val() === today.getFullYear().toString()) {
                    presentOrFutureMonth = $creditCardExpiryMonthElement.val() >= (today.getMonth() + 1);
                }

                return monthAndYearHaveValue && presentOrFutureMonth;
            };

        if (validDateEntry()) {
            isValid = stripe.card.validateExpiry($creditCardExpiryMonthElement.val(), $creditCardExpiryYearElement.val());
        }

        return {
            isValid: isValid,
            errorMessage: stripeErrorMessages.card_error.invalid_expiry,
            $element: $creditCardExpiryMonthElement
        };
    };

    /**
     * Display the month error only if the year has a value
     * @returns {boolean}
     */
    Form.prototype.setDisplayMonthError = function (creditCardExpiryYearElement) {
        this.displayMonthError = creditCardExpiryYearElement.selectedIndex !== 0;
    };

    /**
     * checks the validationProfiles and runs each, stores the error messages and returns if the form is valid and
     * an array of error messages
     * @returns {{isValid: boolean, errors: Array}}
     */
    Form.prototype.isFormValid = function () {
        var errors = [],
            validationProfiles = this.validationProfiles,
            validationProfileResult,
            args,
            validationProfileElem,
            validationProfileArgs;

        validationProfiles.forEach(function (validationProfile) {

            self.checkValidatorExistsException(validationProfile.validator);

            validationProfileElem = validationProfile.elem;
            validationProfileArgs = validationProfile.args && JSON.parse(validationProfile.args);
            args = self.createArgsArray(validationProfileElem, validationProfileArgs);
            validationProfileResult = self[validationProfile.validator].apply(self, args);

            if (!validationProfileResult.isValid) {
                errors.push(validationProfileResult);
            }
        });

        return {
            isValid: !errors.length,
            errors: errors
        };
    };

    /**
     * add 'form-field--error' class to 'form-field' element to give error styles to element
     * add localised error message
     * @param validationResult
     */
    Form.prototype.addErrorDetail = function (validationResult) {
        var $elementParentFormField = utilsHelper.getSpecifiedParent(validationResult.$element, 'form-field');

        if (!validationResult.isValid) {
            $elementParentFormField.addClass('form-field--error');
            if ($(this.getClass('FORM_FIELD_ERROR_MESSAGE'), $elementParentFormField).length === 0) {
                $.create('<p class="' + this.getClass('FORM_FIELD_ERROR_MESSAGE', true) + '">' + validationResult.errorMessage + '</p>')
                    .insertAfter($elementParentFormField[0].lastElementChild);
            }
        } else {
            $elementParentFormField.removeClass('form-field--error');
            $(this.getClass('FORM_FIELD_ERROR_MESSAGE'), $elementParentFormField[0]).remove();
        }
    };

    /**
     * remove message if it exists from error messages array
     * @param message
     */
    Form.prototype.removeMessage = function(message) {
        if (message) {
            var errorIndex = this.errorMessages.indexOf(message);
            if (errorIndex !== -1) {
                this.errorMessages.splice(errorIndex, 1);
            }
        }
    };

    /**
     * recursively loop through an object calling removeMessage on the string value
     * @param objectProperty
     */
    Form.prototype.removeErrorMessages = function (objectProperty) {
        if (Object.prototype.toString.call(objectProperty) === '[object Object]') {
            for (var prop in objectProperty) {
                this.removeErrorMessages(objectProperty[prop]);
            }
        } else if (typeof objectProperty === 'string') {
            this.removeMessage(objectProperty);
        }
    };

    /**
     * this will add or remove the error from the errorMessages array and add error styles to the inputs
     * @param validationResult
     */
    Form.prototype.manageErrors = function (validationResult) {

        var messageIndex = this.errorMessages.indexOf(validationResult.errorMessage);

        this.addErrorDetail(validationResult);

        if (messageIndex === -1 && !validationResult.isValid) {
            //add error
            this.errorMessages.push(validationResult.errorMessage);
        } else if (messageIndex >= 0 && validationResult.isValid) {
            //remove error
            this.errorMessages.splice(messageIndex, 1);

        } else if (messageIndex === -1 && this.errorMessages.length) {

            //make sure that none of the stripe error messages are in the errorMessages array
            for (var stripeProp in stripeErrorMessages) {
                var eMessage = stripeErrorMessages[stripeProp];

                this.removeErrorMessages(eMessage);
            }
        }

        this.displayGlobalErrors(this.errorMessages);
    };

    /**
     * start loading throbber
     */
    Form.prototype.startLoader = function () {
        $(this.getClass('THROBBER'), this.formElement).addClass('js-waiting');
    };

    /**
     * stop loading throbber
     */
    Form.prototype.stopLoader = function () {
        $(this.getClass('THROBBER'), this.formElement).removeClass('js-waiting');
    };

    Form.prototype.setThrobberMessage = function (message) {
        message = message || '';
        $(this.getElem('THROBBER_MESSAGE')).text(message);
    };

    /**
     * set up form validation automatically
     */
    Form.prototype.setupFormValidation = function () {

        var $validation = $('[data-validation]', this.formElement);
        var $creditCardMonthExpiry = $('.js-credit-card-exp-month', this.formElement);
        var $creditCardYearExpiry = $('.js-credit-card-exp-year', this.formElement);
        var elem;
        var validationProfiles = [];

        //reset validationProfiles array
        this.validationProfiles.length = 0;

        for (var i = 0, validationLength = $validation.length; i < validationLength; i++) {
            elem = $validation[i];

            validationProfiles.push({
                elem: elem,
                args: elem.getAttribute('data-arguments'),
                name: elem.getAttribute('data-validation')
            });
        }

        if ($creditCardMonthExpiry.length && $creditCardYearExpiry.length) {
            validationProfiles.push({
                elem: [$creditCardMonthExpiry[0], $creditCardYearExpiry[0]],
                name: 'creditCardExpiry'
            });
        }

        if (validationProfiles.length) {
            this.setupValidation(validationProfiles);
        }
    };

    /**
     * initialise the form, setup the sub listener and set the stripePublishableKey if credit card validation is
     * required
     */
    Form.prototype.init = function () {

        this.setupFormValidation();

        this.submitButton();

        if (self.isStripeForm) {
            stripe.setPublishableKey(guardian.stripePublicKey);
        }
    };

    return Form;
});
