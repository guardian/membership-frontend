define(['lib/bower-components/imager.js/Imager'], function(Imager) {

    var IMAGES_EVENTS = '.delayed-image-load',
        IMAGES_SLIDESHOW = '.js-image-slideshow',
        IMAGES_FEATURE = '.js-image-feature';

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

        if (document.querySelectorAll(IMAGES_FEATURE).length) {
            new Imager(IMAGES_FEATURE, {
                availableWidths: guardian.membership.featureImages.widths,
                availablePixelRatios: guardian.membership.featureImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

    }

    return {
        init: init
    };

});

