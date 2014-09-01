define([
    '$',
    'ajax',
    'stripe',
    'src/modules/tier/JoinPaid',
    'src/utils/form/Form',
    'config/stripeErrorMessages',
    'src/utils/helper'
], function ($, ajax, stripe, JoinPaid, Form, stripeErrorMessages, helper) {

    ajax.init({page: {ajaxUrl: ''}});

    describe('Payment form module', function() {

        var VALID_CREDIT_CARD_NUMBER = '4242424242424242',
            INVALID_CREDIT_CARD_NUMBER = '1234123412341234',
            VALID_CVC_NUMBER = '123',
            EMPTY_STRING = '',
            SUCCESS_POST_URL = '/success/post/url',
            SUCCESS_REDIRECT_URL = '/success/redirect/url',
            YOUR_FORM_HAS_ERRORS = 'Your form has errors',
            joinPaidForm,
            paymentFormFixtureElement,
            canonicalPaymentFormFixtureElement,
            creditCardNumberInputElement,
            creditCardVerificationCodeInputElement,
            submitButtonElement,
            errorMessageDisplayElement,
            creditCardImageElement,
            expiryMonthElement,
            expiryYearElement,
            now,
            addressLineOneElement,
            townElement,
            postcodeElement,
            firstNameElement,
            lastNameElement,
            differentBillingAddress,
            $creditCardNumberInputElementParent,
            $creditCardVerificationCodeInputElementParent,
            $expiryMonthElementParent,
            $firstNameElementParent,
            $addressLineOneElementParent,
            $townElementParent,
            $postcodeElementParent,
            billingAddressFieldSetElement,
            billingAddressLineOneElement,
            billingTownElement,
            billingPostcodeElement,
            $billingAddressLineOneElementParent,
            $billingTownElementParent,
            $billingPostcodeElementParent;

        // PhantomJS doesn't support bind yet
        Function.prototype.bind = Function.prototype.bind || function (context) {
            var fn = this;
            return function () {
                return fn.apply(context, arguments);
            };
        };

        function triggerEvent (element, eventType) {
            var event;
            event = document.createEvent("HTMLEvents");
            event.initEvent(eventType, true, true);
            event.eventName = eventType;
            element.dispatchEvent(event);
        }

        beforeEach(function (done) {
            //pull this in once and cache it
            if (!canonicalPaymentFormFixtureElement) {
                ajax({
                    url: '/base/test/fixtures/paymentForm.fixture.html',
                    method: 'get',
                    success: function (resp) {
                        canonicalPaymentFormFixtureElement = $.create(resp)[0];
                        callback(canonicalPaymentFormFixtureElement);
                    }
                });
            } else {
                callback(canonicalPaymentFormFixtureElement);
            }

            function callback(canonicalPaymentFormFixtureElement) {

                paymentFormFixtureElement = canonicalPaymentFormFixtureElement.cloneNode(true);
                joinPaidForm = new JoinPaid();

                spyOn(joinPaidForm, 'setupForm').and.callFake(function() {
                    joinPaidForm.form = new Form(paymentFormFixtureElement, SUCCESS_POST_URL, SUCCESS_REDIRECT_URL);
                    joinPaidForm.form.init();
                });

                joinPaidForm.init();

                creditCardNumberInputElement = $('.js-credit-card-number', paymentFormFixtureElement)[0];
                creditCardVerificationCodeInputElement = $('.js-credit-card-cvc', paymentFormFixtureElement)[0];
                submitButtonElement = $('.js-submit-input', paymentFormFixtureElement)[0];
                errorMessageDisplayElement = $('.js-payment-errors', paymentFormFixtureElement)[0];
                creditCardImageElement = $('.js-credit-card-image', paymentFormFixtureElement)[0];
                expiryMonthElement = $('.js-credit-card-exp-month', paymentFormFixtureElement)[0];
                expiryYearElement = $('.js-credit-card-exp-year', paymentFormFixtureElement)[0];
                addressLineOneElement = $('.js-address-line-one', paymentFormFixtureElement)[0];
                townElement = $('.js-town', paymentFormFixtureElement)[0];
                firstNameElement = $('.js-name-first', paymentFormFixtureElement)[0];
                lastNameElement = $('.js-name-last', paymentFormFixtureElement)[0];
                postcodeElement = $('.js-post-code', paymentFormFixtureElement)[0];
                differentBillingAddress = $('.js-toggle-billing-address-cta', paymentFormFixtureElement)[0];
                now = new Date();

                done();
            }
        });

        afterEach(function () {
            paymentFormFixtureElement = null;
        });

        it('should correctly initialise itself', function (done) {
            expect(joinPaidForm).toBeDefined();
            expect(joinPaidForm.form.validationProfiles.length).toBe(8);
            expect(joinPaidForm.form.successPostUrl).toEqual(SUCCESS_POST_URL);
            expect(joinPaidForm.form.successRedirectUrl).toEqual(SUCCESS_REDIRECT_URL);

            done();
        });

        it('should detect an invalid credit card number', function (done) {
            var $elementParent = helper.getSpecifiedParent($(creditCardNumberInputElement), 'form-field');
            creditCardNumberInputElement.value = INVALID_CREDIT_CARD_NUMBER;
            triggerEvent(creditCardNumberInputElement, 'blur');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect($elementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_number);

            done();
        });

        it('should allow a valid credit card number', function (done) {
            creditCardNumberInputElement.value = VALID_CREDIT_CARD_NUMBER;
            triggerEvent(creditCardNumberInputElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();

            done();
        });

        it('should detect an invalid Card Verification Code number', function (done) {
            var $elementParent = helper.getSpecifiedParent($(creditCardVerificationCodeInputElement), 'form-field');
            creditCardVerificationCodeInputElement.value = EMPTY_STRING;
            triggerEvent(creditCardVerificationCodeInputElement, 'blur');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect($elementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_cvc);

            done();
        });

        it('should allow a valid Card Verification Code number', function (done) {
            creditCardVerificationCodeInputElement.value = VALID_CVC_NUMBER;
            triggerEvent(creditCardVerificationCodeInputElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();

            done();
        });

        it('no error when year does not have an entry and month does', function (done) {
            expiryMonthElement.value = 2;
            expiryYearElement.selectedIndex = 0;
            triggerEvent(expiryMonthElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();

            done();
        });

        it('error when month does have an entry and year does not', function (done) {
            var $elementParent = helper.getSpecifiedParent($(expiryMonthElement), 'form-field');
            expiryMonthElement.value = 2;
            expiryYearElement.selectedIndex = 0;
            triggerEvent(expiryYearElement, 'change');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect($elementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.invalid_expiry);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeFalsy();

            done();
        });

        it('error when month is in the past', function (done) {

            var $elementParent = helper.getSpecifiedParent($(expiryMonthElement), 'form-field');
            var currentMonth = now.getMonth() + 1;
            var currentYear = now.getFullYear();

            expiryMonthElement.value = currentMonth - 1;
            expiryYearElement.value = currentYear;

            triggerEvent(expiryYearElement, 'change');
            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect($elementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.invalid_expiry);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeFalsy();

            done();
        });

        it('should create and try to submit a stripe customer object', function (done) {

            var paymentDetails = {
                    number: '4242424242424242',
                    cvc: '123',
                    exp_month: '5',
                    exp_year: '2020'
                },
                formDetails = {
                    name: {
                        first: 'captain',
                        last: 'haddock'
                    },
                    address: {
                        lineOne: 'test line one',
                        town: 'test town',
                        postCode: 'T123HJ'
                    }
                };

            spyOn(stripe.card, 'createToken');


            expiryMonthElement.value = paymentDetails.exp_month;
            expiryYearElement.value = paymentDetails.exp_year;
            creditCardNumberInputElement.value = paymentDetails.number;
            creditCardVerificationCodeInputElement.value = paymentDetails.cvc;
            addressLineOneElement.value = formDetails.address.lineOne;
            townElement.value = formDetails.address.town;
            postcodeElement.value = formDetails.address.postCode;
            firstNameElement.value = formDetails.name.first;
            lastNameElement.value = formDetails.name.last;

            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(stripe.card.createToken).toHaveBeenCalled();
            expect(stripe.card.createToken.calls.count()).toEqual(1);
            expect(stripe.card.createToken.calls.argsFor(0)[0]).toEqual(paymentDetails);

            done();
        });

        function getFormInputParents() {
            $creditCardNumberInputElementParent = helper.getSpecifiedParent($(creditCardNumberInputElement), 'form-field');
            $creditCardVerificationCodeInputElementParent = helper.getSpecifiedParent($(creditCardVerificationCodeInputElement), 'form-field');
            $expiryMonthElementParent = helper.getSpecifiedParent($(expiryMonthElement), 'form-field');
            $firstNameElementParent = helper.getSpecifiedParent($(firstNameElement), 'form-field');
            $addressLineOneElementParent = helper.getSpecifiedParent($(addressLineOneElement), 'form-field');
            $townElementParent = helper.getSpecifiedParent($(townElement), 'form-field');
            $postcodeElementParent = helper.getSpecifiedParent($(postcodeElement), 'form-field');
        }

        function getBillingAddressInputsAndParents() {
            billingAddressFieldSetElement = $('.js-billingAddress-fieldset', paymentFormFixtureElement)[0];
            billingAddressLineOneElement = $('.js-address-line-one', billingAddressFieldSetElement)[0];
            billingTownElement = $('.js-town', billingAddressFieldSetElement)[0];
            billingPostcodeElement = $('.js-post-code', billingAddressFieldSetElement)[0];
            $billingAddressLineOneElementParent = helper.getSpecifiedParent($(billingAddressLineOneElement), 'form-field');
            $billingTownElementParent = helper.getSpecifiedParent($(billingTownElement), 'form-field');
            $billingPostcodeElementParent = helper.getSpecifiedParent($(billingPostcodeElement), 'form-field');
        }

        function formErrorAssertions(){
            expect($creditCardNumberInputElementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_number);
            expect($creditCardVerificationCodeInputElementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_cvc);
            expect($expiryMonthElementParent[0].lastElementChild.textContent).toEqual(stripeErrorMessages.card_error.invalid_expiry);
            expect($firstNameElementParent[0].lastElementChild.textContent).toEqual(firstNameElement.getAttribute('data-error-message'));
            expect($addressLineOneElementParent[0].lastElementChild.textContent).toEqual(addressLineOneElement.getAttribute('data-error-message'));
            expect($townElementParent[0].lastElementChild.textContent).toEqual(townElement.getAttribute('data-error-message'));
            expect($postcodeElementParent[0].lastElementChild.textContent).toEqual(postcodeElement.getAttribute('data-error-message'));
        }

        function billingAddressAssertions() {
            expect($billingAddressLineOneElementParent[0].lastElementChild.textContent).toEqual(billingAddressLineOneElement.getAttribute('data-error-message'));
            expect($billingTownElementParent[0].lastElementChild.textContent).toEqual(billingTownElement.getAttribute('data-error-message'));
            expect($billingPostcodeElementParent[0].lastElementChild.textContent).toEqual(billingPostcodeElement.getAttribute('data-error-message'));
        }

        it('should prevent submission of an empty form', function (done) {
            getFormInputParents();

            triggerEvent(paymentFormFixtureElement, 'submit');

            formErrorAssertions();
            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect(joinPaidForm.form.errorMessages.length).toBe(8);

            done();
        });

        it('should prevent submission of an empty form with billing address', function (done) {
            getFormInputParents();

            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            getBillingAddressInputsAndParents();
            formErrorAssertions();
            billingAddressAssertions();

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect(joinPaidForm.form.errorMessages.length).toBe(11);

            done();
        });

        it('should prevent submission of an empty form with billing address and then remove billing address validation when billing address is closed', function (done) {

            //open billing address
            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            getBillingAddressInputsAndParents();
            formErrorAssertions();
            billingAddressAssertions();
            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            expect(joinPaidForm.form.errorMessages.length).toBe(11);

            //close billing address
            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(joinPaidForm.form.errorMessages.length).toBe(8);
            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(YOUR_FORM_HAS_ERRORS);
            formErrorAssertions();

            done();
        });
    });

});

