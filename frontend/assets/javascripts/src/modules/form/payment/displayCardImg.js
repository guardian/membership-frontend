define(['stripe'], function (stripe) {

    var CARD_SPRITE_ICON_PREFIX = 'sprite-card--';
    var CREDIT_CARD_IMG_ELEM = document.querySelector('.js-credit-card-image');
    var regEx = new RegExp('\\b' + CARD_SPRITE_ICON_PREFIX + '\\S+', 'g');

    /**
     * display the credit card image within the credit card input dependant on the stripe lib cardType helper response
     * @param cardNumber
     */
    var displayCardImg = function (cardNumber) {
        var cardType = stripe.cardType(cardNumber).toLowerCase().replace(' ', '-');

        CREDIT_CARD_IMG_ELEM.className = CREDIT_CARD_IMG_ELEM.className.replace(regEx, '');
        CREDIT_CARD_IMG_ELEM.classList.add(CARD_SPRITE_ICON_PREFIX + cardType);
    };

    return displayCardImg;
});
