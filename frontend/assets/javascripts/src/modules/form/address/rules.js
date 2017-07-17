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
    var US_STATE_SELECTOR = '.js-us-state';
    var CA_PROVINCE_SELECTOR = '.js-ca-province';
    var AUS_STATE_SELECTOR = '.js-aus-state';

    /**
     * on load apply the subdivision and country rules
     * add listener on country select to apply the subdivision and country rules
     * these are dependant on there context - currently delivery/billing address fieldsets
     * @param context
     */
    var addRules = function (context) {
        var countrySelect = context.querySelector(COUNTRY_SELECTOR);
        var $countySelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(COUNTY_OR_STATE_SELECTOR), FORM_FIELD_CLASSNAME));
        var $usStateSelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(US_STATE_SELECTOR), FORM_FIELD_CLASSNAME));
        var $caProvinceSelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(CA_PROVINCE_SELECTOR), FORM_FIELD_CLASSNAME));
        var $ausStateSelectParent = $(utilsHelper.getSpecifiedParent(context.querySelector(AUS_STATE_SELECTOR), FORM_FIELD_CLASSNAME));
        countrySelect.addEventListener('change', function (e) {
            var select = e && e.target;
            implementRules(context, select, $countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent);
        });

        implementRules(context, countrySelect, $countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent);
    };

    var implementRules = function (context, select, $countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent) {
        var optionTxt = selectOptionTxt(select);

        validationDisplay.resetErrorState($('[required]', context));
        subdivision.toggle(context, optionTxt, $countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent);
        postcode.toggle(context, optionTxt);
    };

    var selectOptionTxt = function(select) {
        return select.options[select.selectedIndex].textContent.toLowerCase();
    };

    return {
        addRules: addRules
    };

});
