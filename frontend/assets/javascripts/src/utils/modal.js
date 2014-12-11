/**
 * Usage
 *
 * Modal toggle button:
 *      <form action="">
 *          <button class="js-modal-cta" data-modal="js-change-email-modal" type="submit">Change email for me</button>
 *      </form>
 *
 * Modal element:
 *      <div class="modal is-hidden js-modal js-change-email-modal">
 *          <button class="js-modal-confirm">Yes</button>
 *          <button class="js-modal-cancel">No</button>
 *      </div>
 *
 * To use the modal you need a button with the js-modal-cta class, and a modal class specified in the data-modal
 * attribute. The confirm within the modal will perform the buttons forms action, the cancel will remove the modal.
 */
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
