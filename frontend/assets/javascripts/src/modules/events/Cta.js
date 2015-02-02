define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/user'
], function ($, bean, component, user) {
    'use strict';

    var FRIEND = 'Friend';
    var PARTNER = 'Partner';
    var PATRON = 'Patron';
    var TIER_CHANGE_URL = '/tier/change';
    var UPGRADE = 'Upgrade membership';
    var TICKETS_AVAILABLE_SOON = 'Tickets available soon';

    var Cta = function (containerElem) {
        this.elem = containerElem;
    };

    component.define(Cta);

    Cta.prototype.ticketDates = {};

    Cta.prototype.classes = {
        MEMBER_CTA: 'js-member-cta',
        EVENT: 'js-event',
        SALE_START: 'js-ticket-sale-start',
        SALE_START_FRIEND: 'js-ticket-sale-start-friend',
        SALE_START_PARTNER: 'js-ticket-sale-start-partner',
        SALE_START_PATRON: 'js-ticket-sale-start-patron',
        BUY_TICKET_CTA: 'js-ticket-cta',
        TOOLTIP: 'tooltip',
        LEGAL: 'js-legal-terms'
    };

    Cta.prototype.buyTicketCta = function () {

        var salesStart = this.ticketDates.saleStart.getTime();
        var friendSaleStart = this.ticketDates.saleStartFriend.getTime();
        var partnerSaleStart = this.ticketDates.saleStartPartner.getTime();
        var patronSaleStart = this.ticketDates.saleStartPatron.getTime();
        var memberTier = this.memberTier;
        var now = Date.now();
        var $buyTicketsCtaButton = $(this.getElem('BUY_TICKET_CTA'));

        if ($buyTicketsCtaButton.length) {
            if (this.userIsLoggedIn && salesStart < now) {

                // tickets on sale < 7 days
                if (patronSaleStart < now && partnerSaleStart > now) {
                    if (memberTier === PARTNER || memberTier === FRIEND) {
                        this.disableBuyTicketsCtaButton();
                    }
                }

                // tickets on sale > 8 days < 14 days
                if (partnerSaleStart < now && friendSaleStart > now) {
                    if (memberTier === FRIEND) {
                        this.disableBuyTicketsCtaButton();
                    }
                }

                // if we don't have a member tier and the user is logged in and friend sale has not passed
                if (!memberTier && friendSaleStart > now) {
                    this.disableBuyTicketsCtaButton();
                }
            }

            if (!this.userIsLoggedIn && friendSaleStart > now) {
                this.disableBuyTicketsCtaButton();
            }
        }
    };

    Cta.prototype.disableBuyTicketsCtaButton = function () {
        $(this.getElem('BUY_TICKET_CTA')).remove();
        $(this.getElem('LEGAL')).remove();
    };

    Cta.prototype.memberCta = function () {

        var salesStart = this.ticketDates.saleStart.getTime();
        var friendSaleStart = this.ticketDates.saleStartFriend.getTime();
        var partnerSaleStart = this.ticketDates.saleStartPartner.getTime();
        var memberTier = this.memberTier;
        var now = Date.now();
        var $memberCtaElement = $(this.getElem('MEMBER_CTA'));

        if ($memberCtaElement.length) {
            if (this.userIsLoggedIn) {
                if (memberTier) {
                    // tickets not yet on sale
                    if (salesStart > now) {
                        this.removeMemberCtaButton();
                    }
                    // tickets on sale < 7 days
                    if (salesStart < now && partnerSaleStart > now) {
                        if (memberTier === PARTNER) {
                            this.upgradeComingSoonMemberCtaButton();
                        } else if (memberTier === FRIEND) {
                            this.upgradeMemberCtaButton();
                        } else if (memberTier === PATRON) {
                            this.removeMemberCtaButton();
                        }
                    }
                    // tickets on sale > 8 days < 14 days
                    if (partnerSaleStart < now && friendSaleStart > now) {
                        if (memberTier === FRIEND) {
                            this.upgradeMemberCtaButton();
                        } else if (memberTier === PARTNER || memberTier === PATRON) {
                            this.removeMemberCtaButton();
                        }
                    }
                }
            }
        }
    };

    Cta.prototype.upgradeComingSoonMemberCtaButton = function () {
        $(this.getElem('MEMBER_CTA')).text(TICKETS_AVAILABLE_SOON).addClass('is-disabled').removeAttr('href');
    };

    Cta.prototype.upgradeMemberCtaButton = function () {
        $(this.getElem('MEMBER_CTA')).text(UPGRADE).attr('href', TIER_CHANGE_URL);
    };

    Cta.prototype.removeMemberCtaButton = function () {
        $(this.getElem('MEMBER_CTA')).remove();
    };

    Cta.prototype.parseDates = function () {
        this.ticketDates = {
            saleStart: new Date($(this.getElem('SALE_START')).attr('datetime')),
            saleStartFriend: new Date($(this.getElem('SALE_START_FRIEND')).attr('datetime')),
            saleStartPartner: new Date($(this.getElem('SALE_START_PARTNER')).attr('datetime')),
            saleStartPatron: new Date($(this.getElem('SALE_START_PATRON')).attr('datetime'))
        };
    };

    Cta.prototype.init = function () {
        var self = this;
        this.elem = this.elem || this.getElem('EVENT');

        /* buttons are either both not here, both here, or only one is here */
        if (this.getElem('MEMBER_CTA') || this.getElem('BUY_TICKET_CTA')) {

            this.userIsLoggedIn = user.isLoggedIn();

            user.getMemberDetail(function (memberDetail) {
                self.memberTier = memberDetail && memberDetail.tier;
                self.parseDates();
                self.buyTicketCta();
                self.memberCta();
            });
        }
    };

    return Cta;
});
