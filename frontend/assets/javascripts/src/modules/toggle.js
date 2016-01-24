/**
 * Generic toggle component
 * Can be used on any element
 *
 * Example usage:
 *     <button class="js-toggle" data-toggle="js-foo" data-toggle-label="Less foo">
 *         <span class="js-toggle-label">More foo</span>
 *     </button>
 *     <div id="js-foo" data-toggle-hidden>all the foo (initially hidden)</div>
 *
 *     Notes:
 *     * data-toggle-label is optional
 *     * data-toggle-hidden should be added to toggle elements which should be hidden on pageload
 */
define(['$', 'bean'], function ($, bean) {
    'use strict';

    var TOGGLE_BTN_SELECTOR = '.js-toggle',
        TOGGLE_LABEL_SELECTOR = '.js-toggle-label',
        TOGGLE_DATA_ELM = 'toggle',
        TOGGLE_DATA_LABEL = 'toggle-label',
        TOGGLE_DATA_ICON = 'toggle-icon',
        TOGGLE_CLASS = 'is-toggled',
        ELEMENTS_TO_TOGGLE = '[data-toggle-hidden]',
        HIDDEN_CLASS = 'is-hidden';

    var toggleLabel = function($elem) {
        var toggleText = $elem.data(TOGGLE_DATA_LABEL);
        var labelElem = $elem[0].querySelector(TOGGLE_LABEL_SELECTOR);
        if (toggleText) {
            $elem.data(TOGGLE_DATA_LABEL, $elem.text());
            $(labelElem).text(toggleText);
        }
    };

    var toggleIcon = function($elem) {
        var iconName = $elem.data(TOGGLE_DATA_ICON);
        var iconRef = $elem[0].querySelector('svg > use');
        if (iconName && iconRef) {
            $elem.data(TOGGLE_DATA_ICON, iconRef.getAttribute('xlink:href').replace('#icon-', ''));
            iconRef.setAttribute('xlink:href', '#icon-' + iconName);
        }
    };

    var toggleElement = function($elem) {
        return function (e) {
            e.preventDefault();
            var toggleElemId = $elem.data(TOGGLE_DATA_ELM);
            var toggleElem = $(document.getElementById(toggleElemId));

            toggleElem.toggleClass(HIDDEN_CLASS);
            toggleElem.toggleClass(TOGGLE_CLASS);
            $elem.toggleClass(TOGGLE_CLASS);

            toggleIcon($elem);
            toggleLabel($elem);
        };
    };

    var hideToggleElements = function() {
        var toggleContainers = document.querySelectorAll(ELEMENTS_TO_TOGGLE);
        if(toggleContainers.length) {
            [].forEach.call(toggleContainers, function(el) {
                el.classList.add(HIDDEN_CLASS);
            });
        }
    };

    var bindToggles = function() {
        var $toggles = $(TOGGLE_BTN_SELECTOR);
        $toggles.each(function (elem) {
            bean.on(elem, 'click', toggleElement($(elem)));
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
