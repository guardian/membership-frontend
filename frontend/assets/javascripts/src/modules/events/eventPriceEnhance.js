define(['$', 'bonzo', 'src/utils/user'], function ($, bonzo, userUtil) {

    var config = {
        classes: {
            EVENT_PRICE: 'js-event-price',
            EVENT_PRICE_NOTE: 'js-event-price-note',
            EVENT_PRICE_DISCOUNT: 'js-event-price-discount',
            EVENT_SAVING: 'js-event-price-saving',
            EVENT_TRAIL_TAG: 'js-event-price-tag'
        },
        DOM: {}
    };

    var init = function () {
        for (var c in config.classes) {
            if (config.classes.hasOwnProperty(c)) {
                config.DOM = config.DOM || {};
                config.DOM[c] = $(document.querySelector('.' + config.classes[c])); // bonzo object
            }
        }

        userUtil.getMemberDetail(enhanceWithTier);
    };

    var enhanceWithTier = function (memberDetail) {

        var tier = memberDetail && (memberDetail.tier && memberDetail.tier.toLowerCase());

        if (tier && (tier === 'partner' || tier === 'patron')) {
            var priceText = config.DOM.EVENT_PRICE.text(),
                priceDiscount = config.DOM.EVENT_PRICE_DISCOUNT.text();

            if (priceText !== 'Free') {
                config.DOM.EVENT_PRICE.text(priceDiscount);
                config.DOM.EVENT_PRICE_DISCOUNT.text(priceText);
                config.DOM.EVENT_TRAIL_TAG.text('Full price ');
            }
        }
    };

    return {
        init: init
    };
});
