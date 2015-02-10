define([
    'src/utils/helper',
    'src/modules/form/helper/formUtil'
], function (utilsHelper, form) {
    'use strict';

    var FORM_FIELD_ERROR_CLASSNAME = 'form-field--error';
    var IS_HIDDEN_CLASSNAME = 'is-hidden';
    var FORM_FIELD_CLASSNAME = 'form-field';
    var FORM_FIELD_ERROR_MSG_SELECTOR = '.form-field__error-message';
    var FORM_ERR_ELEMS = utilsHelper.toArray(document.querySelectorAll('.js-payment-errors'));

    /**
     * toggle the error state on the inputs fieldset
     * add/remove appropriate error classNames
     * add/remove global form errors
     * show error message or add in input error message if specified
     *
     * @param result
     */
    var toggleErrorState = function (result) {
        var formFieldElem = utilsHelper.getSpecifiedParent(result.elem, FORM_FIELD_CLASSNAME);
        var errMessageElem = formFieldElem.querySelector(FORM_FIELD_ERROR_MSG_SELECTOR);
        var errId = getErrId(result.elem);
        var errIndex = form.errs.indexOf(errId);

        if (!result.isValid) {
            formFieldElem.classList.add(FORM_FIELD_ERROR_CLASSNAME);

            // use the msg provided or fallback to default message
            errMessageElem.textContent = result.msg ? result.msg : errMessageElem.getAttribute('data-default-msg');
            errMessageElem.classList.remove(IS_HIDDEN_CLASSNAME);

            if (errIndex === -1) {
                form.errs.push(errId);
            }
        } else {
            formFieldElem.classList.remove(FORM_FIELD_ERROR_CLASSNAME);
            errMessageElem.classList.add(IS_HIDDEN_CLASSNAME);

            if (errIndex >= 0) {
                form.errs.splice(errIndex, 1);
            }
        }

        displayFormGlobalError(form.errs);
    };

    /**
     * reset the error state on the inputs fieldset
     * remove appropriate error classNames
     * remove global form errors
     * hide input error element
     *
     * We may have errors on elements that are toggled and we need to remove these when the item is hidden
     * - e.g billing address toggle
     * @param $elems
     */
    var resetErrorState = function ($elems) {
        var errIds = [];

        $elems.each(function (elem) {
            if (!elem.hasAttribute('required')) { return; }

            var formField = utilsHelper.getSpecifiedParent(elem, FORM_FIELD_CLASSNAME);
            errIds.push(getErrId(elem));
            formField.classList.remove(FORM_FIELD_ERROR_CLASSNAME);
            formField.querySelector(FORM_FIELD_ERROR_MSG_SELECTOR).classList.add(IS_HIDDEN_CLASSNAME);
        });

        if (errIds.length) {
            flushErrIds(errIds);
        }
    };

    /**
     * display global error messages
     * @param formErrs
     */
    var displayFormGlobalError = function (formErrs) {
        FORM_ERR_ELEMS.map(function (errElem) {
            var addOrRemove = formErrs.length ? 'remove' : 'add';
            errElem.classList[addOrRemove](IS_HIDDEN_CLASSNAME);
        });
    };

    /**
     * to track global errors the name or id of an element is used in the formUtil.errs array
     * @param elem
     * @returns {*}
     */
    var getErrId = function (elem) {
        return elem.name || elem.id;
    };

    /**
     * flush any errors in the global errs array. We may have global from elements that have been toggled and we need
     * to remove these when the item is hidden - e.g billing address toggle
     * @param errsToFlush
     */
    var flushErrIds = function (errsToFlush) {
        form.errs = form.errs.filter(function (err) {
            return errsToFlush.indexOf(err) === -1;
        });

        displayFormGlobalError(form.errs);
    };

    return {
        toggleErrorState: toggleErrorState,
        flushErrIds: flushErrIds,
        resetErrorState: resetErrorState
    };
});
