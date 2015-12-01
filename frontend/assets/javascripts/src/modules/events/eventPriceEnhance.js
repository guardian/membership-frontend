define(['$', 'bonzo', 'src/utils/user'], function ($, bonzo, userUtil) {
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
        if (memberDetail && memberDetail.benefits && memberDetail.benefits.discountedEventTickets) {
            events.each(function(el) {
                updateEventPricing(el);
            });
        }
    };

    var updateEventPricing = function(el) {

        var elPrice = $(el.querySelector(selectors.EVENT_PRICE));
        var elPriceSaving = $(el.querySelector(selectors.EVENT_SAVING));

        if (elPrice.length && elPriceSaving.length) {
            elPrice.text(elPrice.attr('data-discount-text'));
            elPriceSaving.text(elPriceSaving.attr('data-discount-text'));
        }
    };

    return {
        init: init
    };

});
