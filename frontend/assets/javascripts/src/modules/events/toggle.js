// *** generic toggle button component ***
//
// usage:
// <button class="js-toggle action action--toggle" data-toggle="foo">Show more foo</button>
// <div id="foo" class="js-toggle-elm" data-toggle-label="Less foo">all the foo (initially hidden)</div>
// (data-toggle-label is optional)

define(['$', 'bean', 'src/utils/analytics/ga'], function ($, bean, googleAnalytics) {

    var TOGGLE_ELM_SELECTOR = '.js-toggle-elm',
        TOGGLE_BTN_SELECTOR = '.js-toggle',
        TOGGLE_DATA_ELM     = 'toggle',
        TOGGLE_DATA_LABEL   = 'toggle-label',
        TOGGLE_CLASS        = 'is-toggled';

    var toggleElm = function ($elem) {
        // store a ref to the original button text on bind
        var originalText = $elem.text();
        return function () {
            var toggleElmId = $elem.data(TOGGLE_DATA_ELM);
            $(document.getElementById(toggleElmId)).toggle();
            $elem.toggleClass(TOGGLE_CLASS);

            var hasChangedText = ($elem.text() === originalText);
            googleAnalytics.trackEvent('Toggle element', toggleElmId, (hasChangedText ? 'Show' : 'Hide'));
            var toggleText = $elem.data(TOGGLE_DATA_LABEL);
            if (toggleText) {
                $elem.text( hasChangedText ? toggleText : originalText);
            }
        };
    };

    var hideToggleElements = function () {
        var toggleContainers = document.querySelectorAll(TOGGLE_ELM_SELECTOR);
        $(toggleContainers).hide();
    };

    var bindToggles = function () {
        var $toggles = $(TOGGLE_BTN_SELECTOR);
        $toggles.each(function (elem) {
            bean.on(elem, 'click', toggleElm($(elem)));
        });

    };

    function init() {
        hideToggleElements();
        bindToggles();
    }

    init();

});
