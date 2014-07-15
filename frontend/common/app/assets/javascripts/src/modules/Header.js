define([
    '$',
    'bean',
    'src/utils/user'
], function ($, bean, userUtil) {

    //TODO-ben once we bring in components.js from the front end this needs refactoring to work with a scala html fragment

    var config = {
        classes: {
            HTML_ELEMENT: 'html',
            HEADER_JOIN_US_CTA: '.js-header-join-us-cta',
            SECTIONS_POP_UP_JOIN_US_CTA: '.js-sections-nav-join-us',
            SECTIONS_POP_UP_NAV: '.js-sections-nav-popup',
            SECTIONS_POP_UP_PAGE_LIST: '.js-sections-nav-popup-page-list',
            MENU_ICON: '.js-menu-icon',
            IDENTITY_NOTICE: '.identity__notice',
            IDENTITY_ACCOUNT: '.identity__account',
            IDENTITY_TIER: '.identity__tier',
            IDENTITY_ICON: '.js-identity-icon',
            IDENTITY_POP_UP_NAV: '.js-profile-nav-popup',
            COMMENT_ACTIVITY_LINK: '.js-comment-activity'
        },
        text: {
            SIGNED_IN_PREFIX: 'You are signed in as'
        },
        DOM: {}
    };

    function Header() {}

    Header.prototype.init = function() {

        this.cacheDomElements();
        this.populateUserDetails();
        this.addListeners();
    };

    Header.prototype.user = userUtil.getUserFromCookie();

    Header.prototype.cacheDomElements = function () {
        var selector;

        for (selector in config.classes) {
            config.DOM[selector] = $(config.classes[selector]);
        }
    };

    /**
     * Event listeners for the identity menu and the hamburger menu
     */
    Header.prototype.addListeners = function () {
        var self = this;
        var user = this.user;

        bean.on(config.DOM.MENU_ICON[0], 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            config.DOM.IDENTITY_POP_UP_NAV.addClass('is-hidden');
            config.DOM.SECTIONS_POP_UP_NAV.toggleClass('is-hidden');

            if(config.DOM.SECTIONS_POP_UP_NAV.hasClass('is-hidden')) {
                self.removeCloseMenuListener();
            } else {
                self.addCloseMenuListener();
            }
        });

        bean.on(config.DOM.IDENTITY_ICON[0], 'click', function (e) {
            if (user) {
                e.preventDefault();
                e.stopImmediatePropagation();

                config.DOM.SECTIONS_POP_UP_NAV.addClass('is-hidden');
                config.DOM.IDENTITY_POP_UP_NAV.toggleClass('is-hidden');
                config.DOM.IDENTITY_ICON.toggleClass('menu-item--active');

                if(config.DOM.IDENTITY_POP_UP_NAV.hasClass('is-hidden')) {
                    self.removeCloseMenuListener();
                } else {
                    self.addCloseMenuListener();
                }
            }
        });
    };

    /**
     * add a listener on the html element when a menu has been opened,
     * this will close the menu automatically
     */
    Header.prototype.addCloseMenuListener = function () {

        var htmlElement = config.DOM.HTML_ELEMENT;

        bean.on(htmlElement[0], 'click', function () {
            config.DOM.IDENTITY_POP_UP_NAV.addClass('is-hidden');
            config.DOM.SECTIONS_POP_UP_NAV.addClass('is-hidden');
        });
    };

    /**
     * remove the html listener when the menus are closed
     */
    Header.prototype.removeCloseMenuListener = function () {

        var htmlElement = config.DOM.HTML_ELEMENT;

        bean.off(htmlElement[0], 'click');
    };

    /**
     * Populate user details in the header account information container
     */
    Header.prototype.populateUserDetails = function() {
        var user = this.user;

        if (user) {
            config.DOM.IDENTITY_NOTICE.text(config.text.SIGNED_IN_PREFIX).addClass('u-h');
            config.DOM.IDENTITY_ACCOUNT.text(user.displayname).removeClass('u-h');

            userUtil.getMemberTier(function (tier) {
                config.DOM.IDENTITY_TIER.text(tier).removeClass('u-h');
            });

            config.DOM.COMMENT_ACTIVITY_LINK.attr('href', config.DOM.COMMENT_ACTIVITY_LINK.attr('href') + user.id);

        } else {
            config.DOM.HEADER_JOIN_US_CTA.removeClass('is-hidden');
            config.DOM.SECTIONS_POP_UP_JOIN_US_CTA.removeClass('u-h');
            config.DOM.SECTIONS_POP_UP_PAGE_LIST.removeClass('nav--top-border-off');
        }
    };

    return Header;
});
