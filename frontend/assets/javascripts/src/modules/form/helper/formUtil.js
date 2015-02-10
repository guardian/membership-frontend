define([
    'src/utils/helper'
], function (utilsHelper) {

    /**
     * Filter only the elements that require validation, 'input', 'textarea', 'select' input elements that are
     * required or have a validation profile
     * @param elems
     * @returns {*}
     */
    var inputsToValidate = function (elems) {
        return elems.filter(function (elem) {
            var nodeName = elem.nodeName.toLowerCase();
            var inputTypes = ['input', 'textarea', 'select'];
            var required = elem.hasAttribute('required');
            var validationProfile = elem.getAttribute('data-validation');

            return inputTypes.indexOf(nodeName) !== -1 && (required || validationProfile);
        });
    };

    var getFormElem = function () {
        return document.querySelector('.js-form');
    };

    var getInputsToValidate = function (elems) {
        return inputsToValidate(utilsHelper.toArray(elems));
    };

    var hasPayment = function () {
        return !!document.querySelector('.js-credit-card-number');
    };

    /**
     * formUtil singleton provides:
     *    elem: DomElement - the form element
     *    elems: Array - form elements that need validation
     *    hasPayment: Boolean - does the form has payment facilities
     *    errs: Array[String] - current form errors
     *    flush: enables elem and elems flushing for add/remove of validation
     *
     * @returns {{elem, elems, hasPayment, errs: Array, flush: flush}}
     */
    var formUtil = function () {
        var form = getFormElem();

        if (form) {
            return {
                elem: form,
                elems: getInputsToValidate(form.elements),
                hasPayment: hasPayment(),
                errs: [],
                flush: function () {
                    this.elem = getFormElem();
                    this.elems = getInputsToValidate(this.elem.elements);
                }
            };
        }
    };

    return formUtil();
});
