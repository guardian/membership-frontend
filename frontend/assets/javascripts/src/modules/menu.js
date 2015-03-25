define([
    'bean',
    'src/utils/user'
], function (bean, userUtil) {

    var IDENTITY_MENU_CTA_ELEM = document.querySelector('.js-identity-menu-cta');
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

            IDENTITY_MENU_ELEM.classList.toggle('is-hidden');
            IDENTITY_MENU_CTA_ELEM.classList.toggle('is-active');

            if(IDENTITY_MENU_ELEM.classList.contains('is-hidden')) {
                removeDocumentListener();
            } else {
                addDocumentListener();
            }
        });
    }

    function addDocumentListener() {
        bean.on(HTML_ELEM, 'click', function () {
            IDENTITY_MENU_ELEM.classList.add('is-hidden');
        });
    }

    function removeDocumentListener() {
        bean.off(IDENTITY_MENU_ELEM, 'click');
    }

    function setIdentityCtaReturnUrl() {
        var windowLocation = window.location;
        var currentUrl = windowLocation.pathname + windowLocation.search;

        IDENTITY_MENU_CTA_ELEM.setAttribute('href',
            IDENTITY_MENU_CTA_ELEM.getAttribute('href').replace(/(returnUrl=[^&]+)/g, '$1' + currentUrl)
        );
    }

    return {
        init: init
    };
});
