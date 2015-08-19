define([
    'src/modules/form/validation/validity'
], function (validity) {

    var elem = document.createElement('input');
    var pattern = /\s\w+\d{2}/;
    var lengthElem;
    var requiredElem;
    var patternElem;
    var validationProfileElem;

    describe('Test Validity method', function () {

        describe('No Validation', function () {

            it('simple input', function () {
                var standardElem = elem.cloneNode();

                expect(validity.testValidity(standardElem)).toBe(true);
            });

            it('disabled input', function () {
                var disabledElem = elem.cloneNode();
                disabledElem.disabled = true;

                expect(validity.testValidity(disabledElem)).toBe(true);
            });
        });

        describe('Required', function () {

            beforeEach(function () {
                requiredElem = elem.cloneNode();
                requiredElem.setAttribute('required', 'required');
            });

            it('empty value', function () {
                expect(validity.testValidity(requiredElem)).toBe(false);
            });

            it('with value', function () {
                requiredElem.value = 'Henry Dorsett Case'

                expect(validity.testValidity(requiredElem)).toBe(true);
            });
        });

        describe('Length', function () {

            beforeEach(function () {
                lengthElem = elem.cloneNode();
                lengthElem.setAttribute('minlength', 5);
                lengthElem.setAttribute('maxlength', 10);
            });

            it('between min and max', function () {
                lengthElem.value = 'Armitage';

                expect(validity.testValidity(lengthElem)).toBe(true);
            });

            it('below min', function () {
                lengthElem.value = 'Case';

                expect(validity.testValidity(lengthElem)).toBe(false);
            });

            it('above max', function () {
                lengthElem.value = 'Dixie Flatline';

                expect(validity.testValidity(lengthElem)).toBe(false);
            });
        });

        describe('Pattern', function () {

            beforeEach(function () {
                patternElem = elem.cloneNode();
                patternElem.setAttribute('pattern', pattern.source);
            });

            it('match', function () {
                patternElem.value = 'Peter Riviera56';

                expect(validity.testValidity(patternElem)).toBe(true);
            });

            it('no match', function () {
                patternElem.value = 'Peter Riviera';

                expect(validity.testValidity(patternElem)).toBe(false);
            });
        });

        describe('Validation profile', function () {

            beforeEach(function () {
                validationProfileElem = elem.cloneNode();
                window.Stripe = {
                    card: {
                        validateCardNumber: function() {}
                    }
                };
            });

            it('does not exist', function () {
                validationProfileElem.setAttribute('data-validation', 'notAValidationProfile');
                expect(validity.testValidity(validationProfileElem)).toBe(false);
            });

            it('exists', function () {
                spyOn(Stripe.card, 'validateCardNumber').and.returnValue(true);
                validationProfileElem.setAttribute('data-validation', 'validCreditCardNumber');
                expect(validity.testValidity(validationProfileElem)).toBe(true);
            });
        });
    });
});
