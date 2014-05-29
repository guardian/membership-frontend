define(['$', 'src/utils/user'], function ($, userUtil) {

    var config = {
        classes: {
            EVENT_PRICE: 'js-event-price'
        },
        DOM: {},
        MEMBERSHIP_EVENT_DISCOUNT: 0.8 // 20%
    };

    var init = function () {
        for (var c in config.classes) {
            config.DOM = config.DOM || {};
            config.DOM[c] = $(document.querySelector('.' + config.classes[c])); // bonzo object
        }

        userUtil.getMemberTier(enhanceWithTier);
    };

    var enhanceWithTier = function (tier) {
        if (tier && (tier === 'partner' || tier === 'patron')) {
            var price = config.DOM.EVENT_PRICE,
                val = parseInt(price.text().replace('£', ''), 10),
                discountedVal = (val * config.MEMBERSHIP_EVENT_DISCOUNT).toFixed(2);

            price.text('£' + discountedVal);
        }
    };

    return {
        init: init
    };
});