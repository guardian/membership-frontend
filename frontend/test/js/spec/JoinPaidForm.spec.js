define([
    '$',
    'ajax',
    'stripe',
    'src/modules/tier/JoinPaid',
    'src/utils/form/Form',
    'config/stripeErrorMessages'
], function ($, ajax, stripe, JoinPaid, Form, stripeErrorMessages) {

    ajax.init({page: {ajaxUrl: ''}});

    describe('Payment form module', function() {

        var VALID_CREDIT_CARD_NUMBER = '4242424242424242',
            INVALID_CREDIT_CARD_NUMBER = '1234123412341234',
            VALID_CVC_NUMBER = '123',
            EMPTY_STRING = '',
            SUCCESS_POST_URL = '/success/post/url',
            SUCCESS_REDIRECT_URL = '/success/redirect/url',
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
            differentBillingAddress;

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

        beforeEach(function () {

            runs(function () {

                //pull this in once and cache it
                if (!canonicalPaymentFormFixtureElement) {
                    ajax({
                        url: '/base/test/fixtures/paymentForm.fixture.html',
                        method: 'get',
                        success: function (resp) {
                            canonicalPaymentFormFixtureElement = $.create(resp)[0];
                        }
                    });
                }
            });

            waitsFor(function () {
               return !!canonicalPaymentFormFixtureElement;
            }, 'Fixture should be loaded', 1000);

            runs(function () {
                paymentFormFixtureElement = canonicalPaymentFormFixtureElement.cloneNode(true);
                joinPaidForm = new JoinPaid();

                spyOn(joinPaidForm, 'setupForm').andCallFake(function() {
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
            });
        });

        afterEach(function () {
            paymentFormFixtureElement = null;
        });

        it('should correctly initialise itself', function () {
            expect(joinPaidForm).toBeDefined();
            expect(joinPaidForm.form.validationProfiles.length).toBe(8);
            expect(joinPaidForm.form.successPostUrl).toEqual(SUCCESS_POST_URL);
            expect(joinPaidForm.form.successRedirectUrl).toEqual(SUCCESS_REDIRECT_URL);
        });

        it('should detect an invalid credit card number', function () {
            creditCardNumberInputElement.value = INVALID_CREDIT_CARD_NUMBER;
            triggerEvent(creditCardNumberInputElement, 'blur');
            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_number);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });

        it('should allow a valid credit card number', function () {
            creditCardNumberInputElement.value = VALID_CREDIT_CARD_NUMBER;
            triggerEvent(creditCardNumberInputElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();
            expect(submitButtonElement.hasAttribute('disabled')).toBeFalsy();
        });

        it('should detect an invalid Card Verification Code number', function () {
            creditCardVerificationCodeInputElement.value = EMPTY_STRING;
            triggerEvent(creditCardVerificationCodeInputElement, 'blur');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(stripeErrorMessages.card_error.incorrect_cvc);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });

        it('should allow a valid Card Verification Code number', function () {
            creditCardVerificationCodeInputElement.value = VALID_CVC_NUMBER;
            triggerEvent(creditCardVerificationCodeInputElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();
            expect(submitButtonElement.hasAttribute('disabled')).toBeFalsy();
        });

        it('no error when year does not have an entry and month does', function () {
            expiryMonthElement.value = 2;
            expiryYearElement.selectedIndex = 0;
            triggerEvent(expiryMonthElement, 'blur');

            expect(errorMessageDisplayElement.innerHTML).toEqual(EMPTY_STRING);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeTruthy();
            expect(submitButtonElement.hasAttribute('disabled')).toBeFalsy();
        });

        it('error when month does have an entry and year does not', function () {
            expiryMonthElement.value = 2;
            expiryYearElement.selectedIndex = 0;
            triggerEvent(expiryYearElement, 'change');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(stripeErrorMessages.card_error.invalid_expiry);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeFalsy();
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });

        it('error when month is in the past', function () {

            var currentMonth = now.getMonth() + 1,
                currentYear = now.getFullYear();

            expiryMonthElement.value = currentMonth - 1;
            expiryYearElement.value = currentYear;

            triggerEvent(expiryYearElement, 'change');

            expect(errorMessageDisplayElement.firstChild.textContent).toEqual(stripeErrorMessages.card_error.invalid_expiry);
            expect(errorMessageDisplayElement.classList.contains('is-hidden')).toBeFalsy();
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();

        });

        it('should create and try to submit a stripe customer object', function () {

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
            expect(stripe.card.createToken.callCount).toEqual(1);
            expect(stripe.card.createToken.mostRecentCall.args[0]).toEqual(paymentDetails);

        });

        it('should prevent submission of an empty form', function () {
            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(errorMessageDisplayElement.children.length).toBe(8);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });

        it('should prevent submission of an empty form with billing address', function () {
            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(errorMessageDisplayElement.children.length).toBe(11);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });

        it('should prevent submission of an empty form with billing address and then remove billing address validation when billing address is closed', function () {

            //open billing address
            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(errorMessageDisplayElement.children.length).toBe(11);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();

            //close billing address
            triggerEvent(differentBillingAddress, 'click');
            triggerEvent(paymentFormFixtureElement, 'submit');

            expect(errorMessageDisplayElement.children.length).toBe(8);
            expect(submitButtonElement.hasAttribute('disabled')).toBeTruthy();
        });
    });

});

