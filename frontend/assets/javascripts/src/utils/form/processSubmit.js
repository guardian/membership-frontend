/**
 * add loader, processing message and disable submit once pressed
 * Note this is used for submit buttons that are not using Form.js
 */
define(['src/utils/helper'], function (utilsHelper) {

    var FORM_ELEMENT_SELECTOR = '.js-processing-form';
    var FORM_SUBMIT_ELEMENT_SELECTOR = '.js-processing-form-submit';
    var LOADER_CONTAINER_SELECTOR = '.js-loader-container';
    var IS_HIDDEN_CLASS = 'is-hidden';

    var init = function () {
        var formElements = utilsHelper.toArray(document.querySelectorAll(FORM_ELEMENT_SELECTOR));

        if(formElements) {
            formElements.map(function (formElem) {
                formElem.addEventListener('submit', function () {
                    formElem.querySelector(LOADER_CONTAINER_SELECTOR).classList.remove(IS_HIDDEN_CLASS);
                    formElem.querySelector(FORM_SUBMIT_ELEMENT_SELECTOR).setAttribute('disabled', 'disabled');
                }, false);
            });
        }
    };

    return  {
        init: init
    };
});
