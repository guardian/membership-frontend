define([
    '$',
    'bean',
    'src/utils/component',
    'config/appCredentials',
    'src/utils/user',
    'src/utils/masker',
    'stripe',
    'ajax',
    'config/stripeErrorMessages',
    'src/utils/helper'
], function ($, bean, component, appCredentials, userUtil, masker, stripe, ajax, stripeErrorMessages, utilsHelper) {
    'use strict';

    var self;

    function Form (formElement, successPostUrl, successRedirectUrl) {
        self = this;
        this.formElement = formElement;
        this.successPostUrl = successPostUrl;
        this.successRedirectUrl = successRedirectUrl;
    }

    component.define(Form);

    Form.prototype.errorMessages = [];

    Form.prototype.classes = {
        HIDE: 'is-hidden',
        PAYMENT_ERRORS: 'js-payment-errors',
        FORM_SUBMIT: 'js-submit-input',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        CREDIT_CARD_IMAGE: 'js-credit-card-image',
        THROBBER: 'js-waiting-container'
    };

    Form.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Form.prototype.displayMonthError = false;
    Form.prototype.validator = {};
    Form.prototype.listener = {};
    Form.prototype.validationProfiles = [];
    Form.prototype.isStripeForm = false;

    /**
     * build the data object to be sent to the server
     * @param form
     * @param mixin
     * @returns {{}}
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

            if (element.name === '') {
                continue;
            }

            switch (element.nodeName) {
                case 'INPUT':
                    switch (element.type) {
                        case 'tel':
                        case 'text':
                        case 'hidden':
                        case 'password':
                        case 'button':
                        case 'submit':
                            dataObj[element.name] = element.value;
                            break;
                        case 'checkbox':
                        case 'radio':
                            if (element.checked) {
                                dataObj[element.name] = element.value;
                            }
                            break;
                    }
                    break;
                case 'TEXTAREA':
                    dataObj[element.name] = element.value;
                    break;
                case 'SELECT':
                    dataObj[element.name] = element.value;
                    break;
                case 'BUTTON':
                    switch (element.type) {
                        case 'submit':
                        case 'button':
                            dataObj[element.name] = element.value;
                            break;
                    }
                    break;
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
                self.handleErrors([errorMessage]);
            }
        } else {

            var token = response.id;
            var data = self.buildDataObject(self.formElement, {
                stripeToken: token
            });

            ajax({
                url: self.successPostUrl,
                method: 'post',
                data: data,
                success: function () {
                    self.stopLoader();
                    window.location.assign(self.successRedirectUrl);
                },
                error: function (error) {

                    var errorObj,
                        errorMessage;

                    try {
                        errorObj = error.response && JSON.parse(error.response);
                        errorMessage = self.getErrorMessage(errorObj);
                        if (errorMessage) {
                            self.handleErrors([errorMessage]);
                        }
                    } catch (e) {}

                    self.stopLoader();
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
     *
     * @param errorMessages
     */
    Form.prototype.handleErrors = function (errorMessages) {
        var $paymentErrorsElement = $(this.getElem('PAYMENT_ERRORS')),
            $formSubmitButton = $(this.getElem('FORM_SUBMIT')),
            errorString = '';

        if (errorMessages.length) {
            //display errors and disable submit
            errorMessages.forEach(function (element) {
                errorString += '<li>' + element + '</li>';
            });

            $paymentErrorsElement.removeClass(this.classes.HIDE);
            $paymentErrorsElement.html(errorString);

            $formSubmitButton.attr('disabled', true);
        } else {
            //hide errors and enable submit
            $paymentErrorsElement.html('');
            $paymentErrorsElement.addClass(this.classes.HIDE);
            $formSubmitButton.removeAttr('disabled');
        }
    };

    /**
     *
     * @param cardNumber
     * @returns {string}
     */
    Form.prototype.getCardType = function (cardNumber) {
        return stripe.cardType(cardNumber).toLowerCase().replace(' ', '-');
    };

    /**
     *
     * @param creditCardNumber
     */
    Form.prototype.displayCardTypeImage = function (creditCardNumber) {
        var cardType = this.getCardType(creditCardNumber),
            $creditCardImageElement = $(this.getElem('CREDIT_CARD_IMAGE'));

        $creditCardImageElement.attr(this.data.CARD_TYPE, cardType);
    };

    /**
     * add validation to the form
     * @param validationDetail
     * @returns {Form}
     */
    Form.prototype.addValidation = function (validationDetail) {
        var args;

        validationDetail.forEach(function (validator) {

            if (!self[validator.name] && typeof self[validator.name] === 'function') {
                throw 'please specify an existing validation profile';
            }

            utilsHelper.getSpecifiedParent($(validator.elem), 'label').addClass('required-marker');

            args = validator.elem instanceof Array ? validator.elem : [validator.elem];
            self[validator.name].apply(self, args);
        });

        return this;
    };

    /**
     * validation event for required inputs
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
     * validation event for credit card element
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
     * validation event for credit card CVC
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
     * validation event for credit card expiry
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
     * validation event for submit button
     */
    Form.prototype.submitButton = function () {

        bean.on(this.formElement, 'submit', function (e) {
            e.preventDefault();

            // turn month select errors on when submitting
            self.displayMonthError = true;

            var formValidationResult = self.isFormValid();

            if (formValidationResult.isValid) {

                self.startLoader();

                if( self.isStripeForm ){
                    stripe.card.createToken({
                        number: $(self.getElem('CREDIT_CARD_NUMBER')).val(),
                        cvc: $(self.getElem('CREDIT_CARD_CVC')).val(),
                        exp_month: $(self.getElem('CREDIT_CARD_EXPIRY_MONTH')).val(),
                        exp_year: $(self.getElem('CREDIT_CARD_EXPIRY_YEAR')).val()
                    }, self.stripeResponseHandler);
                } else {
                    self.formElement.submit();
                }

            } else {
                formValidationResult.errors.forEach(function (validationProfileResult) {
                    self.manageErrors(validationProfileResult);
                });
            }
        });
    };

    /**
     * validator for required event
     * @param element
     * @returns {{isValid: boolean, errorMessage: *, $element: (*|jQuery|HTMLElement)}}
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
     * validator for credit card event
     * @param creditCardNumberElement
     * @returns {{isValid: ({isValid: *, errorMessage: (config|stripeErrorMessages.card_error.incorrect_number|*), $element: *}|*), errorMessage: (config/stripeErrorMessages.card_error.incorrect_number|*), $element: (*|jQuery|HTMLElement)}}
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
     * @returns {{isValid: ({isValid: *, errorMessage: (config|stripeErrorMessages.card_error.incorrect_cvc|*), $element: *}|*), errorMessage: (config/stripeErrorMessages.card_error.incorrect_cvc|*), $element: (*|jQuery|HTMLElement)}}
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
     * @returns {{isValid: boolean, errorMessage: (config/stripeErrorMessages.card_error.invalid_expiry|*), $element: (*|jQuery|HTMLElement)}}
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
     *
     * @returns {{isValid: boolean, errors: Array}}
     */
    Form.prototype.isFormValid = function () {
        var errors = [],
            validationProfiles = this.validationProfiles,
            validationProfileResult,
            args,
            validationProfileElem;

        validationProfiles.forEach(function (validationProfile) {

            if (self[validationProfile.validator] && typeof self[validationProfile.validator] === 'function') {
                validationProfileElem = validationProfile.elem;
                args = validationProfileElem instanceof Array ? validationProfileElem : [validationProfileElem];
                validationProfileResult = self[validationProfile.validator].apply(self, args);

                if (!validationProfileResult.isValid) {
                    errors.push(validationProfileResult);
                }
            }
        });

        return {
            isValid: !errors.length,
            errors: errors
        };
    };

    /**
     *
     * @param validationResult
     */
    Form.prototype.addErrorStylesForInput = function (validationResult) {
        var $elementParentFormField = utilsHelper.getSpecifiedParent(validationResult.$element, 'form-field');

        if (!validationResult.isValid) {
            $elementParentFormField.addClass('form-field--error');
        } else {
            $elementParentFormField.removeClass('form-field--error');
        }
    };

    /**
     *
     * @param validationResult
     */
    Form.prototype.manageErrors = function (validationResult) {

        var messageIndex = this.errorMessages.indexOf(validationResult.errorMessage);

        this.addErrorStylesForInput(validationResult);

        if (messageIndex === -1 && !validationResult.isValid) {
            //add error
            this.errorMessages.push(validationResult.errorMessage);
        } else if (messageIndex >= 0 && validationResult.isValid) {
            //remove error
            this.errorMessages.splice(messageIndex, 1);
        }

        this.handleErrors(this.errorMessages);
    };


    Form.prototype.startLoader = function () {
        $(this.getElem('THROBBER')).addClass('js-waiting');
    };

    Form.prototype.stopLoader = function () {
        $(this.getElem('THROBBER')).removeClass('js-waiting');
    };

    Form.prototype.init = function () {

        this.submitButton();

        if (self.isStripeForm) {
            stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
        }
    };

    return Form;
});
