define([
    '$',
    'src/utils/helper',
    'src/modules/form/address/subdivision',
    'src/modules/form/address/postcode',
    'src/modules/form/validation/display'
], function ($, utilsHelper, subdivision, postcode, validationDisplay) {
    'use strict';

    var FORM_FIELD_CLASSNAME = 'form-field';
    var COUNTRY_SELECTOR = '.js-country';
    var COUNTY_OR_STATE_SELECTOR = '.js-county-or-state';
    var STATE_SELECTOR = '.js-state';
    var PROVINCE_SELECTOR = '.js-province';

    /**
     * on load apply the subdivision and country rules
     * add listener on country select to apply the subdivision and country rules
     * these are dependant on there context - currently delivery/billing address fieldsets
     * @param context
     */
    var addRules = function (context) {
        var countrySelect = context.querySelector(COUNTRY_SELECTOR);
        var $countySelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(COUNTY_OR_STATE_SELECTOR), FORM_FIELD_CLASSNAME));
        var $stateSelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(STATE_SELECTOR), FORM_FIELD_CLASSNAME));
        var $provinceSelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(PROVINCE_SELECTOR), FORM_FIELD_CLASSNAME));

        countrySelect.addEventListener('change', function (e) {
            var select = e && e.target;
            implementRules(context, select, $countySelectParent, $stateSelectParent, $provinceSelectParent);
        });

        implementRules(context, countrySelect, $countySelectParent, $stateSelectParent, $provinceSelectParent);
    };

    var implementRules = function (context, select, $countySelectParent, $stateSelectParent, $provinceSelectParent) {
        var optionTxt = selectOptionTxt(select);

        validationDisplay.resetErrorState($('[required]', context));
        subdivision.toggle(context, optionTxt, $countySelectParent, $stateSelectParent, $provinceSelectParent);
        postcode.toggle(context, optionTxt);
    };

    var selectOptionTxt = function(select) {
        return select.options[select.selectedIndex].textContent.toLowerCase();
    };

    return {
        addRules: addRules
    };

});
