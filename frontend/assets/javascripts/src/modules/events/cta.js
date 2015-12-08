define([
    '$',
    'src/utils/user'
], function($, userUtil) {
    'use strict';

    var CTA_ELEM = '.js-ticket-cta';
    var CTA_ACTIONS = {
        buy: {},
        join: {
            id: 'join',
            label: 'Become a member',
            href: '/choose-tier'
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

    function init() {
        var elem = $(CTA_ELEM);
        if (elem.length) {
            userUtil.getMemberDetail(function (memberDetail) {
                var memberTier = (memberDetail && memberDetail.tier || 'none').toLowerCase();
                var ctaStatus =  CTA_ACTIONS[$('.js-ticket-sales').data('cta-tier-'+memberTier)];

                if (Object.keys(ctaStatus).length) {
                    enhanceCta(elem, ctaStatus);
                }
            });
        }
    }

    return {
        init: init
    };
});
