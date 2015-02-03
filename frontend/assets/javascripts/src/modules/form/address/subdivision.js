define(['$'], function ($) {
    'use strict';

    var HIDE_CONTENT_VISUALLY_CLASSNAME = 'u-h';
    var UNITED_STATES_STRING = 'united states';
    var CANADA_STRING = 'canada';
    var ZIP_CODE_STRING = 'Zip code';
    var POST_CODE_STRING  = 'Post code';
    var COUNTY_CONTAINER_SELECTOR = '.js-county-container';
    var POSTCODE_LABEL_SELECTOR = '.js-postcode-label';

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
     * @param $stateSelectParent
     * @param $provinceSelectParent
     */
    var toggle = function (context, optionTxt, $countySelectParent, $stateSelectParent, $provinceSelectParent) {
        var $countyContainer = $(COUNTY_CONTAINER_SELECTOR, context);
        var $postcodeLabel = $(POSTCODE_LABEL_SELECTOR, context);

        detachElems([$countySelectParent, $stateSelectParent, $provinceSelectParent]);

        if (optionTxt === UNITED_STATES_STRING) {
            $countyContainer.append($stateSelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $postcodeLabel.text(ZIP_CODE_STRING);
        } else if (optionTxt === CANADA_STRING) {
            $countyContainer.append($provinceSelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $postcodeLabel.text(ZIP_CODE_STRING);
        } else {
            $countyContainer.append($countySelectParent.removeClass(HIDE_CONTENT_VISUALLY_CLASSNAME));
            $postcodeLabel.text(POST_CODE_STRING);
        }
    };

    return {
        toggle: toggle
    };
});
