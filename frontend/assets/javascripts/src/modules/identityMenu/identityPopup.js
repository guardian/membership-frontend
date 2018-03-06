/**
 * This file:
 * Controls the listeners for the identity icon menu found in the header
 * Controls the html document listener to close the identity icon menu
 * Sets the identity icon returnUrl when a user needs to sign in (controlled via JavaScript for caching reasons)
 */
define([
    '$',
    'src/utils/user'
], function ($, userUtil) {
    'use strict';

    var IDENTITY_MENU_CTA_ELEM = $('.js-identity-menu-toggle');
    var IDENTITY_MENU_CTA_URL = $('.js-identity-menu-url');
    var DROPDOWN_DISABLED_CLASS = 'js-dropdown-disabled';

    function init() {
        if (userUtil.isLoggedIn()) {
            disableLink();
        } else {
            disableMenu();
            setIdentityCtaReturnUrl();
        }
    }

    function disableLink() {
        IDENTITY_MENU_CTA_ELEM.on('click', function(e) {
            e.preventDefault();
        });
    }

    function disableMenu() {
        IDENTITY_MENU_CTA_ELEM.addClass(DROPDOWN_DISABLED_CLASS);
    }

    function setIdentityCtaReturnUrl() {
        var windowLocation = window.location;
        var currentUrl = windowLocation.pathname + windowLocation.search;

        if (IDENTITY_MENU_CTA_URL) {
            IDENTITY_MENU_CTA_URL.prop('href',
                populateReturnUrl(IDENTITY_MENU_CTA_ELEM.attr('href'), currentUrl)
            );
        }
    }

    function populateReturnUrl(href, currentUrl) {
        return href.replace(/(returnUrl=[^&]+)/g, '$1' + currentUrl);
    }

    return {
        init: init,
        populateReturnUrl: populateReturnUrl
    };
});
