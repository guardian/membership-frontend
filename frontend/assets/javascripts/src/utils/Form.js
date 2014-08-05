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

    /**
     * This class has grown somewhat, I am happy with the majority of it but parts of it need a bit of a
     * rethink as a few things have been attached for the moment I am leaving it because although verbose it works.
     * TODO-ben: simplify class
     * TODO-ben: move errors to above inputs (confirm with UX and Design)
     */
    var self;

    function Form (formElement, successPostUrl, successRedirectUrl) {
        self = this;
        this.formElement = formElement;
        this.successPostUrl = successPostUrl;
        this.successRedirectUrl = successRedirectUrl;
        this.errorMessages = [];
        this.validationProfiles = [];
    }

    component.define(Form);

    Form.prototype.classes = {
        HIDE: 'is-hidden',
        PAYMENT_ERRORS: 'js-payment-errors',
        FORM_SUBMIT: 'js-submit-input',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        CREDIT_CARD_IMAGE: 'js-credit-card-image',
        THROBBER: 'js-waiting-container',
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
                self.displayErrors([errorMessage]);
            }
        } else {

            var token = response.id;
            var data = self.buildDataObject(self.formElement, {
                'payment.token': token
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
                            self.displayErrors([errorMessage]);
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
     * hide or show error messages in the error element at the top of the page,
     * disable or enable the submit button dependant on if we have errors
     * @param errorMessages
     */
    Form.prototype.displayErrors = function (errorMessages) {
        var $paymentErrorsElement = $(this.getClass('PAYMENT_ERRORS'), this.formElement),
            $formSubmitButton = $(this.getClass('FORM_SUBMIT'), this.formElement),
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
     * add validation to the form,
     * this will add specific validator events to specified elements,
     * it will throw an error if an incorrect validation event method has been supplied
     * @param validationDetail
     * @returns {Form}
     */
    Form.prototype.addValidation = function (validationDetail) {

        var args;
        var validator;

        for (var i = 0, validationDetailLength = validationDetail.length; i < validationDetailLength; i++) {
            validator = validationDetail[i];

            this.checkValidatorExistsException(validator.name);

            utilsHelper.getSpecifiedParent($(validator.elem), 'label').addClass('required-marker');

            args = self.createArgsArray(validator.elem);
            self[validator.name].apply(self, args);
        }

        return this;
    };

    Form.prototype.checkValidatorExistsException = function (validatorName) {
        if (!self[validatorName] && typeof self[validatorName] !== 'function') {
            throw 'please specify an existing validation profile';
        }
    };

    /**
     * remove validator from the validation profile, this takes the validator out of the internal
     * validation profile array and stops messages being displayed and stops this validation being fired on submit
     * @param validationDetail
     *
     * Note to dev: Currently this removes the validationProfile because it is being used for the billing address,
     * when the billing address is closed it is detached from the dom so there is no need to add validation events and
     * remove them, potentially there may be a need for this if this is used for other elements.
     * This also works on name attributes if inputs without name attributes (credit card inputs) need to be
     * removed then this will need a rework.
     */
    Form.prototype.removeValidatorFromValidationProfile = function (validationDetail) {
        var validator;
        var errorMessages = [];
        var validationMessage;
        var args;

        var removeValidators = function (validationProfile, i, validationProfiles) {

            if (validator.elem.name === validationProfile.elem.name &&
                validator.validator === validationProfile.validator) {

                validationProfiles.splice(i, 1);

                // remove any error messages from validation Profiles that are being removed
                if (self.errorMessages.length) {
                    args = self.createArgsArray(validator.elem);
                    validationMessage = self[validator.validator].apply(self, args);
                    errorMessages.push(validationMessage.errorMessage);
                }
            }
        };

        for(var i = 0, validationDetailLength = validationDetail.length; i < validationDetailLength; i++) {
            validator = validationDetail[i];

            this.validationProfiles.map(removeValidators);
        }

        if (errorMessages.length) {
            this.flushErrors(errorMessages);
        }
    };

    /**
     * create arguments array to be used with apply function
     * @param elem
     * @returns {*}
     */
    Form.prototype.createArgsArray = function (elem) {
        return elem instanceof Array ? elem : [elem];
    };

    /**
     * add a validator back in to the validation profile array so messages will be displayed and validation will fire
     * on submit
     * @param validationDetail
     */
    Form.prototype.addValidatorFromValidationProfile = function (validationDetail) {

        for(var i = 0, validationDetailLength = validationDetail.length; i < validationDetailLength; i++) {
            this.validationProfiles.push(validationDetail[i]);
        }
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

                self.startLoader();

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

            this.displayErrors(this.errorMessages);
        }
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
     * checks the validationProfiles and runs each, stores the error messages and returns if the form is valid and
     * an array of error messages
     * @returns {{isValid: boolean, errors: Array}}
     */
    Form.prototype.isFormValid = function () {
        var errors = [],
            validationProfiles = this.validationProfiles,
            validationProfileResult,
            args,
            validationProfileElem;

        validationProfiles.forEach(function (validationProfile) {

            self.checkValidatorExistsException(validationProfile.validator);

            validationProfileElem = validationProfile.elem;
            args = self.createArgsArray(validationProfileElem);
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
     * this will add or remove the error from the errorMessages array and add error styles to the inputs
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

        this.displayErrors(this.errorMessages);
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

    /**
     * set up form validation automatically
     */
    Form.prototype.setupFormValidation = function () {

        var $validation = $('[data-validation]', this.formElement);
        var $creditCardMonthExpiry = $('.js-credit-card-exp-month', this.formElement);
        var $creditCardYearExpiry = $('.js-credit-card-exp-year', this.formElement);
        var elem;
        var validationProfiles = [];

        for (var i = 0, validationLength = $validation.length; i < validationLength; i++) {
            elem = $validation[i];

            validationProfiles.push({
                elem: elem,
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
            this.addValidation(validationProfiles);
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
            stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
        }
    };

    return Form;
});
