/**
 * Generic dropdown component
 * Can be used on any element
 *
 * Example usage:
 *     <button class="js-dropdown" data-dropdown-menu="js-foo"></button>
 *     <div id="js-foo" class="js-dropdown-menu is-hidden">all the foo (initially hidden)</div>
 *
 */
define(['$'], function ($) {
    'use strict';

    var DROPDOWN_CLASS = 'js-dropdown',
        HIDDEN_CLASS = 'is-hidden',
        MENU_SELECTOR = '.js-dropdown-menu',
        DROPDOWN_DISABLED_CLASS = 'js-dropdown-disabled';

    function bindHandlers() {
        $(document).on('click', function(event) {
            var dropdown = closest(event.target, DROPDOWN_CLASS);
            var isHidden;

            if (dropdown && isEnabled(dropdown)) {
                var $menu = $('#'+$(dropdown).data('dropdown-menu'));
                isHidden = $menu.hasClass(HIDDEN_CLASS);
                hideAllMenus();
                if (isHidden) {
                    show($menu);
                }
            } else {
                hideAllMenus();
            }
        });
    }

    function isEnabled(dropdown) {
        return !$(dropdown).hasClass(DROPDOWN_DISABLED_CLASS);
    }

    // bonzo doesn't provide this so had to write my own
    // TODO: Now we've removed bonzo in favour of jQuery, we might use JQ's native function instead
    function closest(elem, className) {
        do {
            if ($(elem).hasClass(className)) {
                return elem;
            }
            elem = elem.parentElement;
        } while (elem);

        return null;
    }

    function hideAllMenus() {
        $(MENU_SELECTOR).addClass(HIDDEN_CLASS);
    }

    function show($elem) {
        $elem.removeClass(HIDDEN_CLASS);
    }

    function init() {
        bindHandlers();
    }

    return {
        init: init
    };

});
