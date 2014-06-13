define(['$', 'bonzo', 'src/utils/user'], function ($, bonzo, userUtil) {

    var config = {
        classes: {
            EVENT_PRICE: 'js-event-price',
            EVENT_PRICE_NOTE: 'js-event-price-note'
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
                priceValue = price.text(),
                priceNote = config.DOM.EVENT_PRICE_NOTE;

            if (priceValue === 'Free') {
                config.DOM.EVENT_PRICE_NOTE.hide();
            } else {
                var val = parseInt(priceValue.replace('£', ''), 10),
                    discountedVal = (val * config.MEMBERSHIP_EVENT_DISCOUNT).toFixed(2);

                price.text(' £' + discountedVal);

                var pre = bonzo(bonzo.create('<span>')).text('£'+val+'').addClass('u-strike');
                var preCont = bonzo(bonzo.create('<span>')).addClass('u-parens old-price').append(pre);

                priceNote.after(preCont);
            }
        }
    };

    return {
        init: init
    };
});