define(['$', 'bonzo', 'src/utils/user', 'lodash/collection/contains'], function ($, bonzo, userUtil, contains) {
    'use strict';

    var selectors = {
        EVENT_CONTAINER: '.js-event-price',
        EVENT_PRICE: '.js-event-price-value',
        EVENT_PRICE_DISCOUNT: '.js-event-price-discount',
        EVENT_SAVING: '.js-event-price-saving'
    };

    var events = $(selectors.EVENT_CONTAINER);

    var init = function () {
        if (events.length) {
            userUtil.getMemberDetail(enhanceWithTier);
        }
    };

    var enhanceWithTier = function (memberDetail) {
        var tier = memberDetail && memberDetail.tier;

        if (tier && contains(memberDetail.discountTicketTiers, tier)) {
            events.each(function(el) {
                updateEventPricing(el);
            });
        }
    };

    var updateEventPricing = function(el) {

        var elPrice = $(el.querySelector(selectors.EVENT_PRICE));
        var elPriceDiscount = $(el.querySelector(selectors.EVENT_PRICE_DISCOUNT));
        var elPriceSaving = $(el.querySelector(selectors.EVENT_SAVING));

        if (elPrice.length && elPriceDiscount.length && elPriceSaving.length) {
            elPrice.text(elPrice.attr('data-discount-text'));
            elPriceDiscount.text(elPriceDiscount.attr('data-discount-text'));
            elPriceSaving.text(elPriceSaving.attr('data-discount-text'));
        }

    };

    return {
        init: init
    };

});
