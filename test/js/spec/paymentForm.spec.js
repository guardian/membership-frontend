define([
    'src/modules/events/form',
    '$'
], function (PaymentForm, $) {

    describe('Payment form module', function() {

        var paymentForm;

        beforeEach(function () {
            paymentForm = new PaymentForm();
        });

        afterEach(function () {
           paymentForm = null;
        });

        /********************************************************
         * Payment - form.js
         ********************************************************/

        xit('should correctly do placeholder test', function() {

            expect(1).toEqual(1);
        });

    });

});

