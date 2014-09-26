define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/user'
], function ($, bean, component, user) {
    'use strict';

    var FRIEND = 'Friend';
    var PARTNER = 'Partner';

    var Cta = function (containerElem) {
        this.elem = containerElem;
    };

    component.define(Cta);

    Cta.prototype.ticketDates = {};

    Cta.prototype.classes = {
        EVENT_TICKETS_CONTAINER: 'event__tickets',
        SALE_START: 'js-ticket-sale-start',
        SALE_START_FRIEND: 'js-ticket-sale-start-friend',
        SALE_START_PARTNER: 'js-ticket-sale-start-partner',
        SALE_START_PATRON: 'js-ticket-sale-start-patron',
        BUY_TICKET_CTA: 'js-ticket-cta',
        TOOLTIP: 'tooltip'
    };

    Cta.prototype.buyTicketCta = function () {

        var salesStart = this.ticketDates.saleStart.getTime();
        var friendSaleStart = this.ticketDates.saleStartFriend.getTime();
        var partnerSaleStart = this.ticketDates.saleStartPartner.getTime();
        var patronSaleStart = this.ticketDates.saleStartPatron.getTime();
        var memberTier = this.memberTier;
        var now = Date.now();

        if (this.userIsLoggedIn && salesStart < now) {

            // tickets on sale < 7 days
            if (patronSaleStart < now && partnerSaleStart > now) {
                if (memberTier === PARTNER || memberTier === FRIEND) {
                    $(this.getClass('BUY_TICKET_CTA'), this.elem).addClass('action--disabled').removeAttr('href');
                }
            }

            // tickets on sale > 8 days < 14 days
            if (partnerSaleStart < now && friendSaleStart > now) {
                if (memberTier === FRIEND) {
                    $(this.getClass('BUY_TICKET_CTA'), this.elem).addClass('action--disabled').removeAttr('href');
                }
            }

            // if we don't have a member tier and the user is logged in and general sale is not released
            if (!memberTier && friendSaleStart > now) {
                $(this.getClass('BUY_TICKET_CTA'), this.elem).addClass('action--disabled').removeAttr('href');
            }
        }

        if (!this.userIsLoggedIn && friendSaleStart > now) {
            $(this.getClass('BUY_TICKET_CTA'), this.elem).addClass('action--disabled').removeAttr('href');
        }
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

        this.elem = this.elem || this.getElem('EVENT_TICKETS_CONTAINER');
        if (this.elem) {
            this.userIsLoggedIn = user.isLoggedIn();

            user.getMemberDetail(function (memberDetail) {
                self.memberTier = memberDetail && memberDetail.tier;
                self.parseDates();
                self.buyTicketCta();
            });
        }
    };

    return Cta;
});
