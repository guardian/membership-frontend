/**
 * This file:
 * Controls the listeners for the identity icon menu found in the header
 * Controls the html document listener to close the identity icon menu
 * Sets the identity icon returnUrl when a user needs to sign in (controlled via JavaScript for caching reasons)
 */
define([
    'bean',
    'src/utils/user'
], function (bean, userUtil) {
    'use strict';

    var IS_HIDDEN = 'is-hidden';
    var IS_ACTIVE = 'is-active';
    var IDENTITY_MENU_CTA_ELEM = document.querySelector('.js-identity-menu-toggle');
    var IDENTITY_MENU_CTA_URL = document.querySelector('.js-identity-menu-url');
    var IDENTITY_MENU_ELEM = document.querySelector('.js-identity-menu');
    var HTML_ELEM = document.documentElement;

    function init() {
        if (userUtil.isLoggedIn()) {
            addMenuListeners();
        } else {
            setIdentityCtaReturnUrl();
        }
    }

    function addMenuListeners() {
        bean.on(IDENTITY_MENU_CTA_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            IDENTITY_MENU_ELEM.classList.toggle(IS_HIDDEN);
            IDENTITY_MENU_CTA_ELEM.classList.toggle(IS_ACTIVE);

            if(IDENTITY_MENU_ELEM.classList.contains(IS_HIDDEN)) {
                removeDocumentListener();
            } else {
                addDocumentListener();
            }
        });
    }

    function addDocumentListener() {
        bean.on(HTML_ELEM, 'click', function () {
            IDENTITY_MENU_ELEM.classList.add(IS_HIDDEN);
        });
    }

    function removeDocumentListener() {
        bean.off(IDENTITY_MENU_ELEM, 'click');
    }

    function setIdentityCtaReturnUrl() {
        var windowLocation = window.location;
        var currentUrl = windowLocation.pathname + windowLocation.search;

        if(IDENTITY_MENU_CTA_URL) {
            IDENTITY_MENU_CTA_URL.setAttribute('href',
                populateReturnUrl(IDENTITY_MENU_CTA_ELEM.getAttribute('href'), currentUrl)
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
