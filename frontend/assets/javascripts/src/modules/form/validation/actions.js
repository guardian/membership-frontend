define([
    '$',
    'bean',
    'src/utils/helper',
    'src/modules/form/helper/formUtil',
    'src/modules/form/validation/listeners'
], function ($, bean, utilsHelper, form, listeners) {
    'use strict';

    var FORM_FIELD_CLASSNAME = 'form-field';
    var OPTIONAL_MARKER_CLASSNAME = 'optional-marker';
    var REQUIRED_ATTRIBUTE_NAME = 'required';
    var ARIA_REQUIRED_ATTRIBUTE_NAME = 'aria-required';
    var LABEL_CLASSNAME = '.label';

    /**
     * add validation to an input
     *   - add required attribute
     *   - add aria required attribute
     *   - remove labels optional className
     *   - add validation listeners
     *   - flush formUtil
     * @param elems
     */
    var addValidation = function (elems) {
        elems.map(function ($elem) {
            var elem = $elem[0];
            $elem.attr(REQUIRED_ATTRIBUTE_NAME, true).attr(ARIA_REQUIRED_ATTRIBUTE_NAME, true);
            $(LABEL_CLASSNAME, utilsHelper.getSpecifiedParent(elem, FORM_FIELD_CLASSNAME)).removeClass(OPTIONAL_MARKER_CLASSNAME);
            listeners.addInputValidationListeners(elem);
        });
        form.flush();
    };

    /**
     * remove validation on an input
     *   - remove data-validation attribute
     *   - remove required attribute
     *   - remove aria required attribute
     *   - remove error classNames
     *   - add labels optional className
     *   - remove validation listeners
     *   - flush formUtil
     * @param elems
     */
    var removeValidation = function (elems) {
        elems.map(function ($elem) {
            var elem = $elem[0];
            var $formField = $(utilsHelper.getSpecifiedParent(elem, FORM_FIELD_CLASSNAME));

            $elem.removeAttr('data-validation').removeAttr(ARIA_REQUIRED_ATTRIBUTE_NAME).removeAttr(REQUIRED_ATTRIBUTE_NAME);
            $(LABEL_CLASSNAME, $formField).addClass(OPTIONAL_MARKER_CLASSNAME).removeClass('form-field--error');
            bean.off(elem, 'blur');
        });
        form.flush();
    };

    return {
        addValidation: addValidation,
        removeValidation: removeValidation
    };
});
