define(['$'], function ($) {
    'use strict';

    var HIDE_CONTENT_VISUALLY_CLASSNAME = 'u-h';
    var UNITED_STATES_STRING = 'united states';
    var CANADA_STRING = 'canada';
    var ZIP_CODE_STRING = 'Zip code';
    var POST_CODE_STRING = 'Post code';
    var COUNTY_STRING = 'County';
    var STATE_STRING = 'State';
    var PROVINCE_STRING = 'Province';
    var COUNTY_CONTAINER_SELECTOR = '.js-county-container';
    var POSTCODE_LABEL_SELECTOR = '.js-postcode-label';
    var AUSTRALIA_STRING = 'australia';
    var OPTIONAL_CLASSNAME = 'optional-marker';
    var TOWN_LABEL_SELECTOR='.js-town-label';
    var TOWN_STRING = 'Town';
    var SUBURB_STRING = 'Suburb';

    var detachElems = function(elems) {
        elems.filter(function ($elem) {
            return $elem.parent().length !== 0;
        }).map(function ($elem) {
            $elem.detach();
        });
    };

    /**
     * show relevant subdivision (county/province/state) dependant on country selected
     * @param context
     * @param optionTxt
     * @param $countySelectParent
     * @param $usStateSelectParent
     * @param $caProvinceSelectParent
     */
    var toggle = function (context, optionTxt, $countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent) {
        var $countyContainer = $(COUNTY_CONTAINER_SELECTOR, context);
        var $postcodeLabel = $(POSTCODE_LABEL_SELECTOR, context);
        var $countyLabel= $('label',$countyContainer);
        var $townLabel = $(TOWN_LABEL_SELECTOR,context);

        detachElems([$countySelectParent, $usStateSelectParent, $caProvinceSelectParent, $ausStateSelectParent]);
        if (optionTxt === UNITED_STATES_STRING) {
            $townLabel.text(TOWN_STRING);
            $countyContainer.append($usStateSelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $postcodeLabel.text(ZIP_CODE_STRING);
            $countyLabel.text(STATE_STRING);
            $countyLabel.removeClass(OPTIONAL_CLASSNAME);
        } else if (optionTxt === CANADA_STRING) {
            $townLabel.text(TOWN_STRING);
            $countyContainer.append($caProvinceSelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $postcodeLabel.text(ZIP_CODE_STRING);
            $countyLabel.text(PROVINCE_STRING);
            $countyLabel.removeClass(OPTIONAL_CLASSNAME);
        } else if (optionTxt === AUSTRALIA_STRING){
            $townLabel.text(SUBURB_STRING);
            $countyContainer.append($ausStateSelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $countyLabel.text(STATE_STRING);
            $countyLabel.removeClass(OPTIONAL_CLASSNAME);
            $postcodeLabel.text(POST_CODE_STRING);
        } else {
            $townLabel.text(TOWN_STRING);
            $countyContainer.append($countySelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            // In the line below we select the current label that is being display in order to update it.
            // More information in: https://github.com/guardian/membership-frontend/pull/1612
            $countyLabel= $('label',$countyContainer);
            $postcodeLabel.text(POST_CODE_STRING);
            $countyLabel.text(COUNTY_STRING);
            $countyLabel.addClass(OPTIONAL_CLASSNAME);
        }
    };

    return {
        toggle: toggle
    };
});
