define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var DOT = '.';
    var MODAL_SELECTOR = '.js-modal';
    var MODAL_CTA_SELECTOR = '.js-modal-cta';
    var MODAL_CONFIRM_SELECTOR = '.js-modal-confirm';
    var MODAL_CANCEL_SELECTOR = '.js-modal-cancel';
    var MODAL_DATA_ATTRIBUTE = 'data-modal';
    var IS_HIDDEN = 'is-hidden';

    var addListeners = function () {

        function removeHtmlListener() {
            bean.off(document.documentElement, 'click');
        }

        function addHtmlListener() {
            bean.on(document.documentElement, 'click', function () {
                $(MODAL_SELECTOR).addClass(IS_HIDDEN);
                removeHtmlListener();
            });
        }

        $(MODAL_CTA_SELECTOR).map(function (button) {

            var $modalElem = $(DOT + button.getAttribute(MODAL_DATA_ATTRIBUTE));
            var modalConfirmElem = $modalElem[0].querySelector(MODAL_CONFIRM_SELECTOR);
            var form;

            bean.on(button, 'click', function (e) {
                e.stop();
                $modalElem.toggleClass(IS_HIDDEN);
                addHtmlListener();

                form = e.target.form;
            });

            bean.on($modalElem[0], 'click', function (e) {
                e.stop();
            });

            bean.on(modalConfirmElem, 'click', function (e) {
                e.stop();
                form.submit();
            });

            bean.on($modalElem[0], 'click', MODAL_CANCEL_SELECTOR, function (e) {
                e.stop();
                $modalElem.toggleClass(IS_HIDDEN);
                removeHtmlListener();
            });
        });
    };

    var init = function () {
        addListeners();
    };

    return {
        init: init
    };
});
