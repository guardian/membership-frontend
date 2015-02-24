define([
    '$',
    'src/utils/user'
], function($, userUtil) {

    var CTA_ELEM = '.js-ticket-cta';

    /**
     * CTA Actions
     *
     * Map properties to status
     * If a property is set to false that attribute won't be updated
     */
    var CTA_ACTIONS = {
        join: {
            label: 'Become a member',
            action: '/join',
            disabled: false
        },
        upgrade: {
            label: 'Upgrade membership',
            action: '/tier/change',
            disabled: false
        },
        unavailable: {
            label: 'Tickets available soon',
            action: false,
            disabled: true
        }
    };

    /**
     * Get sales dates
     *
     * Determine the pre-sale start and end date
     * and all available tier dates.
     */
    function getSalesDates() {

        var tierDates = {
            friend: new Date($('.js-ticket-sale-start-friend').attr('datetime')),
            // Supporter has same sale start date as Friend
            supporter: new Date($('.js-ticket-sale-start-friend').attr('datetime')),
            partner: new Date($('.js-ticket-sale-start-partner').attr('datetime')),
            patron: new Date($('.js-ticket-sale-start-patron').attr('datetime'))
        };

        return {
            preSaleStart: tierDates.patron,
            preSaleEnd: tierDates.friend,
            tiers: tierDates
        };
    }

    /**
     * Get the sale date for the current tier
     *
     * Returns false if there is no tier,
     * or the tier is not eligible for pre-sales
     */
    function getTierSaleDate(tierDates, memberTier) {
        return (memberTier) ? tierDates[memberTier.toLowerCase()] || false : false;
    }

    /**
     * Get new status for CTA
     *
     * 1. If event is bookable return false (don't do anything)
     * 2. If event if off-sale return 'unavailable'
     * 3. If in pre-sale mode and not logged-in return 'join'
     * 4. If in pre-sale, logged-in but tier can't book yet return 'upgrade'
     * 5. If in pre-sale, logged-in and tier can book return false (don't do anything)
     */
    function newStatus(preSaleStart, preSaleEnd, tierDate) {
        var status = false;

        var now = Date.now();
        var isOffSale = (now < preSaleStart.getTime());
        var isPreSale = (now > preSaleStart.getTime() && now < preSaleEnd.getTime());

        /**
         * Event is not on-sale
         */
        if (isOffSale) {
            status = 'unavailable';
        }

        /**
         * Event is in pre-sale mode
         */
        if (isPreSale) {
            status = 'join';
            /**
             * We have a sale date for the tier
             * (false if we are signed-out or have no sale-date)
             */
            if (tierDate) {
                /**
                 * Current tier's sale date is in the future
                 */
                if (tierDate > now) {
                    status = 'upgrade';
                } else {
                    /**
                     * Current tier's sale date is in the present,
                     * so is bookable. Leave the button alone
                     */
                    status = false;
                }
            }
        }

        return status;
    }

    /**
     * Enhance CTA
     *
     * Updates label, action or disabled status depending on status.
     */
    function enhanceCta(elem, ctaStatus) {
        var newLabel = CTA_ACTIONS[ctaStatus].label || false,
            newAction = CTA_ACTIONS[ctaStatus].action || false,
            shouldDisable = CTA_ACTIONS[ctaStatus].disabled || false;

        if (newLabel) {
            elem.text(newLabel);
        }

        if (newAction) {
            elem.attr('href', newAction);
        }

        if (shouldDisable) {
            elem.addClass('is-disabled').attr('disabled', true);
        }
    }

    function init() {
        var elem = $(CTA_ELEM);
        if (elem.length) {
            userUtil.getMemberDetail(function (memberDetail) {

                var memberTier = memberDetail && memberDetail.tier,
                    ticketSales = getSalesDates(),
                    tierSaleDate = getTierSaleDate(ticketSales.tiers, memberTier),
                    ctaStatus = newStatus(ticketSales.preSaleStart, ticketSales.preSaleEnd, tierSaleDate);

                if (ctaStatus) {
                    enhanceCta(elem, ctaStatus);
                }

            });
        }
    }

    return {
        newStatus: newStatus,
        init: init
    };

});
