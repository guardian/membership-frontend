// *** generic toggle button component ***
//
// usage:
// <button class="js-toggle action action--toggle" data-toggle="foo" data-toggle-label="Less foo">Show more foo</button>
// <div id="foo" class="js-toggle-elm">all the foo (initially hidden)</div>
// (data-toggle-label is optional)

define(['$', 'bean', 'src/utils/analytics/ga'], function ($, bean, googleAnalytics) {

    var TOGGLE_ELM_SELECTOR = '.js-toggle-elm',
        TOGGLE_BTN_SELECTOR = '.js-toggle',
        TOGGLE_DATA_ELM     = 'toggle',
        TOGGLE_DATA_LABEL   = 'toggle-label',
        TOGGLE_CLASS        = 'is-toggled';

    var toggleElm = function ($elem) {
        return function () {
            var toggleElmId = $elem.data(TOGGLE_DATA_ELM);

            $(document.getElementById(toggleElmId)).toggle();
            $elem.toggleClass(TOGGLE_CLASS);

            toggleLabel($elem);

            trackUsage($elem, toggleElmId);
        };
    };

    var toggleLabel = function($elem) {
        var toggleText = $elem.data(TOGGLE_DATA_LABEL);
        if (toggleText) {
            $elem.data(TOGGLE_DATA_LABEL, $elem.text());
            $elem.text(toggleText);
        }
    };

    var trackUsage = function($elem, id) {
        var hasToggled = ($elem.hasClass(TOGGLE_CLASS));
        googleAnalytics.trackEvent('Toggle element', id, (hasToggled ? 'Show' : 'Hide'));
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

    return {
        init: init
    };

});
