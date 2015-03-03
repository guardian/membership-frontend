define([
    '$',
    'src/utils/user'
], function($, userUtil) {

    var CTA_ELEM = '.js-ticket-cta';
    var CTA_ACTIONS = {
        join: {
            id: 'join',
            label: 'Become a member',
            href: '/join'
        },
        upgrade: {
            id: 'upgrade',
            label: 'Upgrade membership',
            href: '/tier/change'
        },
        unavailable: {
            id: 'unavailable',
            label: 'Tickets available soon',
            disabled: true
        }
    };

    /**
     * Determine the ticket sale start and end date. Get all available tier ticket dates.
     */
    function getTicketSaleDates() {

        function dateTime(selector) {
            return new Date($(selector).attr('datetime'));
        }

        var tiersSaleStartDates = {
            friend: dateTime('.js-ticket-sale-start-friend'),
            supporter: dateTime('.js-ticket-sale-start-friend'), // Supporter has same sale start date as Friend
            partner: dateTime('.js-ticket-sale-start-partner'),
            patron: dateTime('.js-ticket-sale-start-patron')
        };

        return {
            priorityBookingStart: tiersSaleStartDates.patron,
            priorityBookingEnd: tiersSaleStartDates.friend,
            tiersSaleStart: tiersSaleStartDates
        };
    }

    /**
     * Get status object for CTA
     *
     * Rules for the CTA
     * - If event is not on sale to anyone return 'unavailable' status object
     * - If event is within priority booking mode and user is not logged-in return 'join' status object
     * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the
     * future return 'upgrade' status object
     * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the
     * past return empty status object
     * - If event is on sale to everyone, and user is logged-in with or without a tier return empty status object
     *
     * @param priorityBookingStart
     * @param priorityBookingEnd
     * @param memberTierTicketSaleStart
     * @returns {*}
     */
    function getCtaStatus(priorityBookingStart, priorityBookingEnd, memberTierTicketSaleStart) {
        var status = {};
        var now = Date.now();
        var eventNotOnSale = (now < priorityBookingStart.getTime());
        var eventWithinPriorityBookingWindow = (now > priorityBookingStart.getTime() && now < priorityBookingEnd.getTime());

        if (eventNotOnSale) {
            status = CTA_ACTIONS.unavailable;
        }

        if (eventWithinPriorityBookingWindow) {
            status = CTA_ACTIONS.join;

            if (memberTierTicketSaleStart) {
                if (memberTierTicketSaleStart > now) {
                    status = CTA_ACTIONS.upgrade; // Tickets are not yet on sale for this tier
                } else {
                    status = {};
                }
            }
        }

        return status;
    }

    /**
     * Updates label, action or disabled status depending on status.
     *
     * @param elem
     * @param ctaStatus
     */
    function enhanceCta(elem, ctaStatus) {
        elem.text(ctaStatus.label);

        if (ctaStatus.href) {
            elem.attr('href', ctaStatus.href);
        }

        if (ctaStatus.disable) {
            elem.addClass('is-disabled').attr('disabled', true);
        }
    }

    /**
     * Get a members ticket sale start date or return undefined
     * @param memberDetail
     * @param ticketSaleDates
     * @returns {*|memberDetails.tier}
     */
    function getMemberTierTicketSaleStart(memberDetail, ticketSaleDates) {
        var memberTier = memberDetail && memberDetail.tier;
        return memberTier && ticketSaleDates.tiersSaleStart[memberTier.toLowerCase()];
    }

    function init() {
        var elem = $(CTA_ELEM);
        if (elem.length) {
            userUtil.getMemberDetail(function (memberDetail) {

                var ticketSaleDates = getTicketSaleDates();
                var ctaStatus = getCtaStatus(
                    ticketSaleDates.priorityBookingStart,
                    ticketSaleDates.priorityBookingEnd,
                    getMemberTierTicketSaleStart(memberDetail, ticketSaleDates)
                );

                if (Object.keys(ctaStatus).length) {
                    enhanceCta(elem, ctaStatus);
                }
            });
        }
    }

    return {
        getCtaStatus: getCtaStatus,
        init: init
    };
});
