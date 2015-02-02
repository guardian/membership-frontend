define([
    'src/modules/form/validation',
    'src/modules/form/helper/formUtil',
    'src/modules/form/payment',
    'src/modules/form/address',
    'src/modules/form/helper/password'
], function (validation, form, payment, address, password) {
    'use strict';

    var init = function () {
        if (form) {
            validation.init();
            address.init();
            password.init();

            if (form.hasPayment) {
                payment.init();
            }
        }
    };

    return {
        init: init
    };
});
