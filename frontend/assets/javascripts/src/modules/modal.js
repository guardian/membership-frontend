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
    '$'
], function ($) {
    'use strict';

    var MODAL_SELECTOR = '.js-modal';
    var MODAL_CTA_SELECTOR = '.js-modal-cta';
    var MODAL_CONFIRM_SELECTOR = '.js-modal-confirm';
    var MODAL_CANCEL_SELECTOR = '.js-modal-cancel';
    var MODAL_DATA_ATTRIBUTE = 'data-modal';
    var IS_HIDDEN = 'is-hidden';
    var ESC_KEY_CODE = 27;

    var addListeners = function () {

        function removeHtmlListener() {
            $(document.documentElement).off('click.modal');
            $(document).off('keydown.modal');
        }

        function addHtmlListener() {
            $(document.documentElement).on('click.modal', function () {
                $(MODAL_SELECTOR).addClass(IS_HIDDEN);
                removeHtmlListener();
            });

            $(document).on('keydown.modal', function(e) {
                var $modals = $(MODAL_SELECTOR);
                if (!$modals.hasClass(IS_HIDDEN) && e.keyCode === ESC_KEY_CODE) {
                    $modals.addClass(IS_HIDDEN);
                }
                removeHtmlListener();
            });
        }

        function toggleClass(elem, className) {
            $(elem).toggleClass(className);
        }

        $(MODAL_CTA_SELECTOR).map(function (_, button) {

            var modalElem = document.querySelector('.' + button.getAttribute(MODAL_DATA_ATTRIBUTE));
            var modalConfirmElem = modalElem.querySelector(MODAL_CONFIRM_SELECTOR);
            var form;

            $(button).on('click', function (e) {
                e.stopPropagation();
                toggleClass(modalElem, IS_HIDDEN);
                addHtmlListener();

                form = e.target.form;
            });

            // stop propagation when clicking modal element
            $(modalElem).on('click.modal', function (e) {
                e.stopPropagation();
            });

            $(modalConfirmElem).on('click.modal', function (e) {
                e.stopPropagation();
                form.submit();
            });

            $(modalElem).on('click.modal', MODAL_CANCEL_SELECTOR, function (e) {
                e.stopPropagation();
                toggleClass(modalElem, IS_HIDDEN);
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
