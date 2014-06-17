define([
    '$',
    'bean',
    'config/appCredentials',
    'src/utils/user',
    'src/utils/masker',
    'stripe',
    'ajax',
    'config/stripeErrorMessages'
], function ($, bean, appCredentials, userUtil, masker, stripe, ajax, stripeErrorMessages) {
    'use strict';

    function StripePaymentForm () {}

    StripePaymentForm.prototype.config = {
        classes: {
            STRIPE_FORM: 'js-stripe-form',
            FORM_FIELD_ERROR: 'form-field--error',
            ERROR_CARD_NUMBER: 'js-error--card-number',
            ERROR_CARD_CVC: 'js-error--card-cvc',
            ERROR_CARD_EXPIRY: 'js-error--card-expiry',
            HIDE: 'is-hidden',
            PAYMENT_ERRORS: 'js-payment-errors',
            FORM_SUBMIT: 'js-submit-input',
            CREDIT_CARD_NUMBER: 'js-credit-card-number',
            CREDIT_CARD_CVC: 'js-credit-card-cvc',
            CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
            CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
            CREDIT_CARD_IMAGE: 'js-credit-card-image',
            CREDIT_CARD: 'credit-card--active',
            TIER_FIELD: 'js-tier-field',
            ACTIONS: 'js-waiting-container'
        },
        DOM: {},
        data: {
            CARD_TYPE: 'data-card-type'
        },
        url: {
            SUBSCRIBE: '/subscription/subscribe'
        }
    };

   StripePaymentForm.prototype.displayMonthError = false;

    /**
    * get parent by className
    * @param $element
    * @param parentClass
    * @returns {*}
    */
    StripePaymentForm.prototype.getSpecifiedParent = function ($element, parentClass) {

        do {
            $element = $element.parent();

        } while ($element && !$element.hasClass(parentClass));

        return $element;
    };

    StripePaymentForm.prototype.domElementSetup = function () {
        for (var className in this.config.classes) {
            this.config.DOM[className] = this.context.querySelector('.' + this.config.classes[className]);
        }
    };

    StripePaymentForm.prototype.getElement = function (element) {
        return $(this.config.DOM[element]);
    };

    StripePaymentForm.prototype.stripeResponseHandler = function (status, response) {

        var self = this,
            config = this.config;

        if (response.error) {
            var errorMessage = self.getErrorMessage(response.error);
            if (errorMessage) {
                self.handleErrors([errorMessage]);
            }
        } else {

            // token contains id, last4, and card type
            var token = response.id;

            ajax({
                url: config.url.SUBSCRIBE,
                method: 'post',
                data: {
                    stripeToken: token,
                    tier: self.getElement('TIER_FIELD').val()
                },
                success: function () {
                    self.stopLoader();
                    window.location = window.location.href.replace('payment', 'thankyou');
                },
                error: function (error) {

                    var errorObj,
                        errorMessage;

                    try {
                        errorObj = error.response && JSON.parse(error.response);
                    } catch (e) {}

                    self.stopLoader();
                    errorMessage = self.getErrorMessage(errorObj);
                    if (errorMessage) {
                        self.handleErrors([errorMessage]);
                    }
                }
            });
        }
    };

   /**
    * get friendly error messages via codes sent from stripe
    * @param errorObj {type: 'card_error', code: 'incorrect_number', decline_code: 'do_not_honour'}
    * @returns {*}
    */
    StripePaymentForm.prototype.getErrorMessage = function (errorObj) {

        var errorMessage;

        try {

            if (errorObj.code === 'card_declined') {
                errorMessage = stripeErrorMessages[errorObj.type][errorObj.code][errorObj.decline_code];
                if (!errorMessage) {
                    errorMessage = stripeErrorMessages[errorObj.type].processing_error;
                }
            } else {
                errorMessage = stripeErrorMessages[errorObj.type][errorObj.code];
            }
        } catch(e) {}

        return errorMessage;
    };

   /**
    *
    * @param errorMessages
    */
    StripePaymentForm.prototype.handleErrors = function (errorMessages) {
        var $paymentErrorsElement = this.getElement('PAYMENT_ERRORS'),
            $formSubmitButton = this.getElement('FORM_SUBMIT'),
            errorString = '';

        if (errorMessages.length) {
            //display errors and disable submit
            errorMessages.forEach(function (element) {
                errorString += element + '\n';
            });

            $paymentErrorsElement.removeClass(this.config.classes.HIDE);
            $paymentErrorsElement.html(errorString);

            $formSubmitButton.attr('disabled', true);
        } else {
            //hide errors and enable submit
            $paymentErrorsElement.html('');
            $paymentErrorsElement.addClass(this.config.classes.HIDE);
            $formSubmitButton.removeAttr('disabled');
        }
    };

    StripePaymentForm.prototype.getCardType = function (cardNumber) {
        return stripe.cardType(cardNumber).toLowerCase().replace(' ', '-');
    };

    StripePaymentForm.prototype.displayCardTypeImage = function (creditCardNumber) {
        var cardType = this.getCardType(creditCardNumber),
            $creditCardImageElement = this.getElement('CREDIT_CARD_IMAGE');

        $creditCardImageElement.attr(this.config.data.CARD_TYPE, cardType);
    };

    StripePaymentForm.prototype.addListeners = function () {
        var stripePaymentFormClass = this,
            $creditCardNumberElement = this.getElement('CREDIT_CARD_NUMBER'),
            $creditCardCVCElement = this.getElement('CREDIT_CARD_CVC'),
            $creditCardExpiryMonthElement = this.getElement('CREDIT_CARD_EXPIRY_MONTH'),
            $creditCardExpiryYearElement = this.getElement('CREDIT_CARD_EXPIRY_YEAR'),
            $formElement = $(this.context);

        bean.on($creditCardNumberElement[0], 'keyup blur', function () {
            masker(' ', 4).bind(this)();
            stripePaymentFormClass.displayCardTypeImage($creditCardNumberElement.val());
        });

        bean.on($creditCardNumberElement[0], 'blur', function (e) {

            var $creditCardNumberElement = $(e.target),
                validationResult = this.validateCardNumber($creditCardNumberElement);

            this.manageErrors(validationResult);

        }.bind(this));

        bean.on($creditCardCVCElement[0], 'blur', function (e) {

            var $cvcElement = $(e.target),
                validationResult = this.validateCVC($cvcElement);

            this.manageErrors(validationResult);

        }.bind(this));

        bean.on($creditCardExpiryMonthElement[0], 'blur', function (e) {

            this.setDisplayMonthErrorStatus();

            var $expiryMonthElement = $(e.target),
                validationResult = this.validateExpiry($expiryMonthElement);

            this.manageErrors(validationResult);

        }.bind(this));

        bean.on($creditCardExpiryYearElement[0], 'blur', function (e) {

            this.displayMonthError = true;

            var $expiryYearElement = $(e.target),
                validationResult = this.validateExpiry($expiryYearElement);

            this.manageErrors(validationResult);

        }.bind(this));

        bean.on($formElement[0], 'submit', function (e) {
            e.preventDefault();

            // turn month select errors on when submitting
            this.displayMonthError = true;

            var formValidationResult = this.isFormValid();

            if (formValidationResult.isValid) {

                this.startLoader();

                stripe.card.createToken({
                    number: $creditCardNumberElement.val(),
                    cvc: $creditCardCVCElement.val(),
                    exp_month: $creditCardExpiryMonthElement.val(),
                    exp_year: $creditCardExpiryYearElement.val()
                }, this.stripeResponseHandler.bind(this));

            } else {
               formValidationResult.errors.forEach(function (validationProfileResult) {
                    this.manageErrors(validationProfileResult);
                }, this);
            }
        }.bind(this));
    };

    /**
    *
    * @param $creditCardNumberElement
    * @returns {{isValid: *, errorMessage: (config/stripeErrorMessages.card_error.incorrect_number|*), $element: *}}
    */
    StripePaymentForm.prototype.validateCardNumber = function ($creditCardNumberElement) {

        var $element = $creditCardNumberElement || this.getElement('CREDIT_CARD_NUMBER');

        return {
            isValid: stripe.card.validateCardNumber($element.val()),
            errorMessage: stripeErrorMessages.card_error.incorrect_number,
            $element: $element
        };
    };

    /**
    *
    * @param $cvcElement
    * @returns {{isValid: *, errorMessage: (config/stripeErrorMessages.card_error.incorrect_cvc|*), $element: *}}
    */
    StripePaymentForm.prototype.validateCVC = function ($cvcElement) {

        var $element = $cvcElement || this.getElement('CREDIT_CARD_CVC');

        return {
            isValid: stripe.card.validateCVC($element.val()),
            errorMessage: stripeErrorMessages.card_error.incorrect_cvc,
            $element: $element
        };
    };

    /**
    * Display the month error only if the year has a value
    * @returns {boolean}
    */
    StripePaymentForm.prototype.setDisplayMonthErrorStatus = function () {
        var $creditCardExpiryYearElement = this.getElement('CREDIT_CARD_EXPIRY_YEAR');

        this.displayMonthError = $creditCardExpiryYearElement[0].selectedIndex === 0 ? false : true;
    };



    /**
    *
    * @returns {{isValid: boolean, errorMessage: (config/stripeErrorMessages.card_error.invalid_expiry|*), $element: *}}
    */
    StripePaymentForm.prototype.validateExpiry = function () {

        var $creditCardExpiryMonthElement = this.getElement('CREDIT_CARD_EXPIRY_MONTH'),
            $creditCardExpiryYearElement = this.getElement('CREDIT_CARD_EXPIRY_YEAR'),
            today = new Date(),
            isValid = !this.displayMonthError,
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
    *
    * @returns {{isValid: boolean, errors: Array}}
    */
    StripePaymentForm.prototype.isFormValid = function () {

        var errors = [],
            validationProfiles = [
                this.validateCardNumber,
                this.validateCVC,
                this.validateExpiry
            ];

        for (var profile in validationProfiles) {

            var validationProfile = validationProfiles[profile];

            if (validationProfiles.hasOwnProperty(profile) && 'function' === typeof validationProfile) {

                var validationProfileResult = validationProfile.call(this);

                if (!validationProfileResult.isValid) {
                    errors.push(validationProfileResult);
                }
            }
        }

        return {
            isValid: !errors.length,
            errors: errors
        };
    };

    /**
    *
    * @param validationResult
    */
    StripePaymentForm.prototype.addErrorStyles = function (validationResult) {
        var $elementParentFormField = this.getSpecifiedParent(validationResult.$element, 'form-field');

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
    StripePaymentForm.prototype.manageErrors = function (validationResult) {

        var paymentErrorsElement = this.getElement('PAYMENT_ERRORS')[0],
            paymentErrorsElementText = paymentErrorsElement.textContent,
            errors = [],
            messageIndex,
            standardFormErrors = [
                stripeErrorMessages.card_error.invalid_expiry,
                stripeErrorMessages.card_error.incorrect_cvc,
                stripeErrorMessages.card_error.incorrect_number
            ];

        this.addErrorStyles(validationResult);

        if (paymentErrorsElementText !== '') {
            //split error element text on '\n' and remove any empty elements from resulting array
            errors = paymentErrorsElementText.split('\n').filter(function (element) { return element.length !== 0; });
        }

        /*
        remove any errors that are not the standard form errors, errors returned from stripe are removed when elements
        are blurred
         */
        if (validationResult.isValid) {
            errors = errors.filter(function (errorMessage) { return standardFormErrors.indexOf(errorMessage) !== -1; });
        }

        messageIndex = errors.indexOf(validationResult.errorMessage);

        if (messageIndex === -1 && !validationResult.isValid) {
            //add error
            errors.push(validationResult.errorMessage);
        } else if (messageIndex >= 0 && validationResult.isValid) {
            //remove error
            errors.splice(messageIndex, 1);
        }

        this.handleErrors(errors);
    };

    StripePaymentForm.prototype.startLoader = function () {
        this.getElement('ACTIONS').addClass('js-waiting');
    };

    StripePaymentForm.prototype.stopLoader = function () {
        this.getElement('ACTIONS').removeClass('js-waiting');
    };

    StripePaymentForm.prototype.init = function (context) {

        this.context = context || document.querySelector('.' + this.config.classes.STRIPE_FORM);

        if (!this.context.className.match(this.config.classes.STRIPE_FORM)) {
            this.context = this.context.document.querySelector('.' + this.config.classes.STRIPE_FORM);
        }

        if (this.context) {
            this.domElementSetup();

            this.addListeners();

            stripe.setPublishableKey(appCredentials.stripe.stripePublishableKey);
        }
    };

    return StripePaymentForm;
});