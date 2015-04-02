/**
 * loader and processing message utilities
 */
define(function() {
    'use strict';

    var LOADER_ELEM = document.querySelector('.js-loader');
    var SUBMIT_SELECTOR = '.js-submit-input';
    var LOADING = 'is-loading';

    var startLoader = function () {
        LOADER_ELEM.classList.add(LOADING);
    };

    var stopLoader = function () {
        LOADER_ELEM.classList.remove(LOADING);
    };

    var setProcessingMessage = function (msg) {
        LOADER_ELEM.textContent = msg;
    };

    var disableSubmitButton = function (shouldDisable) {
        var submitEl = document.querySelector(SUBMIT_SELECTOR);
        if(shouldDisable) {
            submitEl.setAttribute('disabled', true);
        } else {
            submitEl.removeAttribute('disabled');
        }
    };

    return {
        disableSubmitButton: disableSubmitButton,
        setProcessingMessage: setProcessingMessage,
        startLoader: startLoader,
        stopLoader: stopLoader
    };
});
