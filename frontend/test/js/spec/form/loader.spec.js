define(['$', 'src/modules/form/helper/loader'], function ($, loader) {

    describe('disableSubmitButton', function () {

        var SUBMIT_CLASSNAME = 'js-submit-input';
        var SUBMIT_SELECTOR = '.js-submit-input';
        var FIXTURE_ID = 'disableSubmitButton';

        var fixture = [
            '<input class="' + SUBMIT_CLASSNAME + '" type="submit" value="Submit">'
        ].join('');

        function addFixtures(member) {
            $(document.body).append('<div id="' + FIXTURE_ID + '">' + fixture + '</div>');
        }

        function removeFixtures() {
            $('#' + FIXTURE_ID).remove();
        }

        beforeEach(function () {
            addFixtures();
        });

        afterEach(function () {
            removeFixtures();
        });

        it('should disabled an input', function () {
            var inputElem = $(SUBMIT_SELECTOR);
            loader.disableSubmitButton(true);
            expect(inputElem.val()).toEqual('Submit');
            expect(inputElem.attr('disabled')).toBe(true);
        });

        it('should re-enabled previously disabled input', function () {
            var inputElem = $(SUBMIT_SELECTOR);
            inputElem.attr('disabled', true);
            expect(inputElem.attr('disabled')).toBe(true);
            loader.disableSubmitButton(false);
            expect(inputElem.val()).toEqual('Submit');
            expect(inputElem.attr('disabled')).toBe(false);
        });

    });
});
