define([
    'bean',
    'src/modules/form/helper/formUtil',
    'src/modules/form/helper/loader',
    'src/modules/form/payment/processing',
    'src/modules/form/validation/validity'
], function (bean, form, loader, processing, validity) {
    'use strict';

    var SUBMIT_ELEM = document.querySelector('.js-submit-input');

    var addInputValidationListeners = function (elem) {
        // blur and change events for select elems, just blur for everything else (which is just text inputs at present)
        var event = elem.nodeName.toLowerCase() === 'select' ? 'blur change' : 'blur';

        bean.on(elem, event, function () {
            validity.check(elem);
        });
    };

    /**
     * add submit button listener
     * error check the form
     * show appropriate loading/processing messages
     * disable submit button
     * if form has payment capabilities for get the stripe process under way,
     * if the form does not have payment capabilities then just submit the form
     */
    var addSubmitListener = function () {
        bean.on(SUBMIT_ELEM, 'click', function (e) {
            var processingMessage = 'Processing...';
            e.preventDefault();

            form.elems.map(function (elem) {
                validity.check(elem);
            });

            if (!form.errs.length) {
                if (form.hasPayment) {
                    processingMessage.replace('...', ' payment...');
                }

                loader.startLoader();
                loader.setProcessingMessage(processingMessage);
                loader.disableSubmitButton(true);

                if (form.hasPayment) {
                    processing.getStripeToken();
                } else {
                    form.elem.submit();
                }
            }
        });
    };

    return {
        addSubmitListener: addSubmitListener,
        addInputValidationListeners: addInputValidationListeners
    };
});
