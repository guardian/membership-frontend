// *** generic toggle button component ***
//
// usage:
// <button class="js-toggle" data-toggle="foo">Show more foo</button>
// <div id="foo" class="js-toggle-elm">all the foo (initially hidden)</div>

define(['$', 'bean', 'src/utils/analytics/ga'], function ($, bean, googleAnalytics) {

    var TOGGLE_ELM_SELECTOR = '.js-toggle-elm';
    var TOGGLE_BTN_SELECTOR = '.js-toggle';
    var TOGGLE_DATA_ATTR    = 'toggle';
    var ICON_ON_CLASS       = 'action--plus-left';
    var ICON_OFF_CLASS      = 'action--minus-left';
    var BUTTON_OFF_TEXT     = 'Less';

    var toggleElm = function (elm, textOriginal) {
        return function () {
            var toggleElmId = elm.data(TOGGLE_DATA_ATTR);
            $(document.getElementById(toggleElmId)).toggle();
            elm.toggleClass([ICON_ON_CLASS, ICON_OFF_CLASS].join(' '));
            var hasChangedText = (elm.text() === textOriginal);
            googleAnalytics.trackEvent('Toggle element', toggleElmId, (hasChangedText ? 'Show' : 'Hide'));
            elm.text( hasChangedText ? BUTTON_OFF_TEXT : textOriginal);
        };
    };

    var hideToggleElements = function () {
        var e = document.querySelectorAll(TOGGLE_ELM_SELECTOR);
        for (var i=0, l=e.length; i<l; i++) {
            $(e[i]).hide();
        }
    };

    var bindToggles = function () {
        var toggles = document.querySelectorAll(TOGGLE_BTN_SELECTOR);

        for (var i=0, l=toggles.length; i<l; i++) {
            var elm = toggles[i];
            // store a ref to the original button text on pageload
            var textOriginal = $(elm).text();
            bean.on(elm, 'click', toggleElm($(elm), textOriginal));
        }
    };

    function init() {
        hideToggleElements();
        bindToggles();
    }

    init();

});
