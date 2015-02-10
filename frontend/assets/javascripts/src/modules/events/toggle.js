// *** generic toggle button component ***
//
// usage:
//     <button class="js-toggle" data-toggle="js-foo" data-toggle-label="Less foo">More foo</button>
//     <div id="js-foo" data-toggle-hidden>all the foo (initially hidden)</div>
// notes:
//     * data-toggle-label is optional.
//     * data-toggle-hidden should be added to toggle elements which should be hidden on pageload

define(['$', 'bean'], function ($, bean) {

    var TOGGLE_BTN_SELECTOR = '.js-toggle',
        TOGGLE_DATA_ELM     = 'toggle',
        TOGGLE_DATA_LABEL   = 'toggle-label',
        TOGGLE_CLASS        = 'is-toggled',
        ELEMENTS_TO_TOGGLE  = '[data-toggle-hidden]';

    var toggleElm = function ($elem) {
        return function (e) {
            e.preventDefault();
            var toggleElmId = $elem.data(TOGGLE_DATA_ELM);

            $(document.getElementById(toggleElmId)).toggle();
            $elem.toggleClass(TOGGLE_CLASS);

            toggleLabel($elem);
        };
    };

    var toggleLabel = function($elem) {
        var toggleText = $elem.data(TOGGLE_DATA_LABEL);
        if (toggleText) {
            $elem.data(TOGGLE_DATA_LABEL, $elem.text());
            $elem.text(toggleText);
        }
    };

    var hideToggleElements = function () {
        var toggleContainers = document.querySelectorAll(ELEMENTS_TO_TOGGLE);
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
