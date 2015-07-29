/**
 * This is a stand-alone component.
 * Add loader, processing message and disable submit once pressed.
 * Note this is used for submit buttons that are not using Form.js
 */
define([
    'src/utils/helper',
    'src/modules/form/helper/loader'
], function (utilsHelper, loader) {
    'use strict';

    var FORM_ELEMENT_SELECTOR = '.js-processing-form';
    var FORM_SUBMIT_ELEMENT_SELECTOR = '.js-processing-form-submit';

    var init = function() {
        var formElements = utilsHelper.toArray(document.querySelectorAll(FORM_ELEMENT_SELECTOR));

        if(formElements) {
            formElements.map(function(formElem) {
                formElem.addEventListener('submit', function() {
                    loader.startLoader();
                    formElem.querySelector(FORM_SUBMIT_ELEMENT_SELECTOR).setAttribute('disabled', 'disabled');
                }, false);
            });
        }
    };

    return {
        init: init
    };
});
