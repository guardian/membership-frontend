define([
    '$',
    'bean',
    'src/utils/user',
    'src/utils/helper'
], function ($, bean, userUtil, utilsHelper) {

    //TODO-ben once we bring in components.js from the front end this needs refactoring to work with a scala html fragment

    var config = {
        classes: {
            DOCUMENT_ELEMENT: 'html',
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
        this.appendLocationDetailToIdentityReturnUrl();
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

            self.setMenuListener.call(self, config.DOM.SECTIONS_POP_UP_NAV);
        });

        bean.on(config.DOM.IDENTITY_ICON[0], 'click', function (e) {
            if (user) {
                e.preventDefault();
                e.stopImmediatePropagation();

                config.DOM.SECTIONS_POP_UP_NAV.addClass('is-hidden');
                config.DOM.IDENTITY_POP_UP_NAV.toggleClass('is-hidden');
                config.DOM.IDENTITY_ICON.toggleClass('menu-item--active');

                self.setMenuListener.call(self, config.DOM.IDENTITY_POP_UP_NAV);
            }
        });
    };

    Header.prototype.setMenuListener = function (navElement) {
        if(navElement.hasClass('is-hidden')) {
            this.removeCloseMenuListener();
        } else {
            this.addCloseMenuListener();
        }
    };

    /**
     * add a listener on the html element when a menu has been opened,
     * this will close the menu automatically
     */
    Header.prototype.addCloseMenuListener = function () {
        bean.on(config.DOM.DOCUMENT_ELEMENT[0], 'click', function () {
            config.DOM.IDENTITY_POP_UP_NAV.addClass('is-hidden');
            config.DOM.SECTIONS_POP_UP_NAV.addClass('is-hidden');
        });
    };

    /**
     * remove the html listener when the menus are closed
     */
    Header.prototype.removeCloseMenuListener = function () {
        bean.off(config.DOM.DOCUMENT_ELEMENT[0], 'click');
    };

    /**
     * Populate user details in the header account information container,
     * If a Identity user is logged in then populate the user details found in the header.
     */
    Header.prototype.populateUserDetails = function() {
        var user = this.user;

        if (user) {
            config.DOM.IDENTITY_NOTICE.text(config.text.SIGNED_IN_PREFIX).addClass('u-h');
            config.DOM.IDENTITY_ACCOUNT.text(user.displayname).removeClass('u-h');

            userUtil.getMemberDetail(function (memberDetail) {
                var tier = memberDetail && (memberDetail.tier && memberDetail.tier.toLowerCase());
                if (tier) {
                    config.DOM.IDENTITY_TIER.text(tier).removeClass('u-h');
                }
            });

            config.DOM.COMMENT_ACTIVITY_LINK.attr('href', config.DOM.COMMENT_ACTIVITY_LINK.attr('href') + user.id);

        } else {
            config.DOM.HEADER_JOIN_US_CTA.removeClass('is-hidden');
            config.DOM.SECTIONS_POP_UP_JOIN_US_CTA.removeClass('u-h');
            config.DOM.SECTIONS_POP_UP_PAGE_LIST.removeClass('nav--top-border-off');
        }
    };

    Header.prototype.appendLocationDetailToIdentityReturnUrl = function () {
        config.DOM.IDENTITY_ICON.attr('href', config.DOM.IDENTITY_ICON.attr('href') + utilsHelper.getLocationDetail());
    };

    return Header;
});
