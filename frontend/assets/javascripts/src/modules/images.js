define(['lib/bower-components/imager.js/Imager'], function(Imager) {

    function init() {

        var imageEvents = '.delayed-image-load';
        if (document.querySelectorAll(imageEvents).length) {
            new Imager(imageEvents, {
                availableWidths: guardian.membership.eventImages.widths,
                availablePixelRatios: guardian.membership.eventImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

        var imageSlideshow = '.js-image-slideshow';
        if (document.querySelectorAll(imageSlideshow).length) {
            new Imager(imageSlideshow, {
                availableWidths: guardian.membership.homeImages.widths,
                availablePixelRatios: guardian.membership.homeImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

        var imageFeature = '.js-image-feature';
        if (document.querySelectorAll(imageFeature).length) {
            new Imager(imageFeature, {
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

