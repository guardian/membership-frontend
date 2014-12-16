/**
 * add throbber, processing message and disable submit once pressed
 */
define(['$', 'bean'], function ($, bean) {

    var FORM_ELEMENT_SELECTOR = '.js-form';
    var FORM_SUBMIT_ELEMENT_SELECTOR = '.js-form-submit';
    var THROBBER_CONTAINER_SELECTOR = '.js-throbber-container';
    var IS_HIDDEN_CLASS = 'is-hidden';

    return function () {
        var $formElements = $(FORM_ELEMENT_SELECTOR);

        if($formElements.length) {
            $formElements.map(function (formElem) {
                bean.on(formElem, 'submit.throbber', function () {
                    $(formElem.querySelector(THROBBER_CONTAINER_SELECTOR)).removeClass(IS_HIDDEN_CLASS);
                    $(formElem.querySelector(FORM_SUBMIT_ELEMENT_SELECTOR)).attr('disabled', true);
                });
            });
        }
    };
});
