define(['lib/bower-components/imager.js/Imager'], function(Imager) {

    var IMAGES_EVENTS = '.delayed-image-load';
    var IMAGES_SLIDESHOW = '.js-image-slideshow';

    function init() {

        if (document.querySelectorAll(IMAGES_EVENTS).length) {
            new Imager(IMAGES_EVENTS, {
                availableWidths: guardian.membership.eventImages.widths,
                availablePixelRatios: guardian.membership.eventImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

        if (document.querySelectorAll(IMAGES_SLIDESHOW).length) {
            new Imager(IMAGES_SLIDESHOW, {
                availableWidths: guardian.membership.homeImages.widths,
                availablePixelRatios: guardian.membership.homeImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

    }

    return {
        init: init
    };

});

