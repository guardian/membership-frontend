define([
    'src/modules/events/form',
    'ajax',
    '$',
    'stripe'
], function (PaymentForm, ajax, $, stripe) {

    ajax.init({page: {ajaxUrl: ''}});

    describe('Payment form module', function() {

        var paymentForm, paymentFormFixture = null, errorMessageContainer;

        // PhantomJS doesn't support bind yet
        Function.prototype.bind = Function.prototype.bind || function (thisp) {
            var fn = this;
            return function () {
                return fn.apply(thisp, arguments);
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

            var ajaxFlag = false;

            runs(function () {
                ajax({
                    url: '/base/test/fixtures/paymentForm.fixture.html',
                    method: 'get',
                    success: function (resp) {
                        paymentFormFixture = $.create(resp);
                    }
                });
            });

            waitsFor(function () {
               return paymentFormFixture !== null;
            }, 'Fixture should be loaded', 1000);

            runs(function () {
                paymentForm = new PaymentForm();
                paymentForm.init(paymentFormFixture[0]);
                errorMessageContainer = paymentFormFixture[0].querySelector('.js-payment-errors');
            });

        });

        afterEach(function () {
           paymentForm = null;
           paymentFormFixture = null;
           errorMessageContainer = null;
        });

        /********************************************************
         * Payment - form.js
         ********************************************************/

        xit('should correctly initialise itself', function () {
            expect(paymentForm.context).toEqual(paymentFormFixture[0]);
            expect(paymentForm.config.DOM.CREDIT_CARD_NUMBER).toEqual(paymentFormFixture[0].querySelector('.js-credit-card-number'));
        });

        xit('should display an error message', function () {
            var testMessage = 'a test error message'
            paymentForm.handleError(testMessage);
            expect(paymentFormFixture[0].querySelector('.js-payment-errors').innerHTML).toEqual(testMessage);
        });

        /********************************************************
         *  Testing the validation for the CC number here,
         *  all depends on Stripe so no reason to test the CVC and expiry...
         ********************************************************/

        xit('should detect an invalid credit card number', function () {
            var ccNumInput = paymentFormFixture[0].querySelector('.js-credit-card-number');
            ccNumInput.value = "123";
            triggerEvent(ccNumInput, 'blur');
            expect(errorMessageContainer.innerHTML).toEqual('Please enter a valid card number');
        });

        xit('should allow a valid credit card number', function () {
            var ccNumInput = paymentFormFixture[0].querySelector('.js-credit-card-number');
            ccNumInput.value = "4242424242424242";
            triggerEvent(ccNumInput, 'blur');
            expect(errorMessageContainer.innerHTML).toEqual(''); // no error message
            expect(errorMessageContainer.className.match('hide').length).toEqual(1); // hidden class applied
        });

        xit('should prevent submission of an empty form', function () {
            var formElement = paymentFormFixture[0];
            triggerEvent(formElement, 'submit');
            expect(errorMessageContainer.innerHTML).toEqual('Please enter a valid card number, Please enter a valid CVC number, Please enter a valid Expiry')
        });

        /********************************************************
         * Submission
         ********************************************************/

        xit('should create and try to submit a stripe customer object', function () {

            spyOn(stripe.card, 'createToken'); // http://tobyho.com/2011/12/15/jasmine-spy-cheatsheet/

            var paymentDetails = {
                number : '4242424242424242',
                cvc : '123',
                exp_month : '5',
                exp_year : '2020'
            };

            paymentFormFixture[0].querySelector('.js-credit-card-number').value     = paymentDetails.number;
            paymentFormFixture[0].querySelector('.js-credit-card-cvc').value        = paymentDetails.cvc;
            paymentFormFixture[0].querySelector('.js-credit-card-exp-month').value  = paymentDetails.exp_month;
            paymentFormFixture[0].querySelector('.js-credit-card-exp-year').value   = paymentDetails.exp_year;

            var formElement = paymentFormFixture[0];
            triggerEvent(formElement, 'submit');

            expect(stripe.card.createToken.mostRecentCall.args[0]).toEqual(paymentDetails);
        });

    });

});

