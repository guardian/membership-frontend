define([
    'src/modules/form/helper/formUtil',
    'src/modules/form/validation/listeners'
], function (form, listeners) {
    'use strict';

    /**
     * Initialise validation
     * Add validation listeners for inputs
     * Add form submit button listener
     */
    var init = function () {
        form.elems.map(function (elem) {
            listeners.addInputValidationListeners(elem);
        });
        listeners.addSubmitListener();
    };

    return {
        init: init
    };
});
