define(['src/utils/user'], function (userUtil) {

    var MENU_TOGGLE_SELECTOR = '.js-menu-icon';
    var NAVIGATION_SELECTOR ='.js-global-nav';
    var NAVIGATION_SUB_SELECTOR ='.js-sub-nav';
    var MEMBERS_AREA_SELECTOR = '.js-members-area';

    var ACTIVE_CLASS = 'is-active';
    var HIDDEN_CLASS = 'is-hidden';

    function addListeners(menuToggleEl) {

        var navigationEl = document.querySelector(NAVIGATION_SELECTOR),
            navigationSubEl = document.querySelector(NAVIGATION_SUB_SELECTOR);

        menuToggleEl.addEventListener('click', function(event) {
            event.preventDefault();
            menuToggleEl.classList.toggle(ACTIVE_CLASS);
            if(navigationEl) {
                navigationEl.classList.toggle(ACTIVE_CLASS);
            }
            if(navigationSubEl) {
                navigationSubEl.classList.toggle(ACTIVE_CLASS);
            }
        });
    }

    function showMembersArea() {
        var membersAreaLink = document.querySelector(MEMBERS_AREA_SELECTOR);
        if (userUtil.getUserFromCookie() && membersAreaLink) {
            membersAreaLink.classList.remove(HIDDEN_CLASS);
        }
    }

    function init() {
        var menuToggleEl = document.querySelector(MENU_TOGGLE_SELECTOR);
        if (menuToggleEl) {
            addListeners(menuToggleEl);
            showMembersArea();
        }
    }

    return {
        init: init
    };

});
