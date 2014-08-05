define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/user',
    'src/utils/helper'
], function ($, bean, component, user, utilsHelper) {
    'use strict';

    var FRIEND = 'Friend';
    var PARTNER = 'Partner';
    var PATRON = 'Patron';
    var Cta = function () {};

    component.define(Cta);

    Cta.prototype.ticketDates = {};

    Cta.prototype.classes = {
        BOOKING_DATES_CONTAINER: 'js-booking-dates',
        EVENT_TICKETS_CONTAINER: 'event__tickets',
        SALE_START: 'js-ticket-sale-start',
        SALE_START_FRIEND: 'js-ticket-sale-start-friend',
        SALE_START_PARTNER: 'js-ticket-sale-start-partner',
        SALE_START_PATRON: 'js-ticket-sale-start-patron',
        MEMBER_CTA: 'js-member-cta',
        BUY_TICKET_CTA: 'js-ticket-cta',
        TICKET_SIGN_IN_MESSAGE: 'js-ticket-sale-sign-in',
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
                    $(this.getElem('BUY_TICKET_CTA')).addClass('action--disabled').removeAttr('href');
                }
            }

            // tickets on sale > 8 days < 14 days
            if (partnerSaleStart < now && friendSaleStart > now) {
                if (memberTier === FRIEND) {
                    $(this.getElem('BUY_TICKET_CTA')).addClass('action--disabled').removeAttr('href');
                }
            }

            if (!memberTier) {
                $(this.getElem('BUY_TICKET_CTA')).addClass('action--disabled').removeAttr('href');
            }
        }

        if (!this.userIsLoggedIn && friendSaleStart > now) {
            $(this.getElem('BUY_TICKET_CTA')).addClass('action--disabled').removeAttr('href');
        }
    };

    Cta.prototype.memberCta = function () {

        var memberTier = this.memberTier;

        if (this.userIsLoggedIn) {
            if (memberTier === PATRON) {
                $(this.getElem('MEMBER_CTA')).addClass('u-h');
            } else {
                $(this.getElem('MEMBER_CTA')).text('Upgrade').attr('href', '/tier/change');
            }
        }
    };

    Cta.prototype.existingMembersSignInMessage = function () {
        if (!this.userIsLoggedIn) {
            $(this.getElem('TICKET_SIGN_IN_MESSAGE'))
                .attr('href', $(this.getElem('TICKET_SIGN_IN_MESSAGE')).attr('href') + utilsHelper.getLocationDetail())
                .parent().removeClass('u-h');
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

    Cta.prototype.addTooltipListener = function () {
        var self = this;

        bean.on($(this.getElem('TOOLTIP'))[0], 'mouseenter', function () {
            $(self.getElem('BOOKING_DATES_CONTAINER')).removeClass('u-h');
        });

        bean.on($(this.getElem('TOOLTIP'))[0], 'mouseleave', function () {
            $(self.getElem('BOOKING_DATES_CONTAINER')).addClass('u-h');
        });
    };

    Cta.prototype.init = function () {
        var self = this;

        this.elem = this.getElem('EVENT_TICKETS_CONTAINER');
        this.userIsLoggedIn = user.isLoggedIn();

        user.getMemberDetail(function (memberDetail, err) {
            if (!err) {
                self.memberTier = memberDetail && memberDetail.tier;
                self.parseDates();
                self.buyTicketCta();
                self.memberCta();
                self.existingMembersSignInMessage();
                self.addTooltipListener();
            }
        });
    };

    return Cta;
});
