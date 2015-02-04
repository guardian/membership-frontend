/**
 * loader and processing message utilities
 */
define(['$'], function ($) {
    'use strict';

    var JS_WAITING_CLASSNAME = 'js-waiting';
    var SUBMIT_BTN_SELECTOR = '.js-submit-input';
    var LOADER_ELEM = document.querySelector('.js-waiting-container');
    var LOADER_MSG_ELEM = document.querySelector('.js-waiting-message');

    var startLoader = function () {
        LOADER_ELEM.classList.add(JS_WAITING_CLASSNAME);
    };

    var stopLoader = function () {
        LOADER_ELEM.classList.remove(JS_WAITING_CLASSNAME);
    };

    var setProcessingMessage = function (msg) {
        LOADER_MSG_ELEM.textContent = msg;
    };

    var disableSubmitButton = function (isDisabled) {
        $(SUBMIT_BTN_SELECTOR).attr('disabled', !!isDisabled);
    };

    return {
        disableSubmitButton: disableSubmitButton,
        setProcessingMessage: setProcessingMessage,
        startLoader: startLoader,
        stopLoader: stopLoader
    };
});
