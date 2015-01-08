define(['lib/bower-components/imager.js/Imager', 'src/utils/helper'], function(Imager, utilsHelper) {

    var IMAGES_EVENTS = '.delayed-image-load';
    var IMAGE_LOADED_IMAGE = '.imager-loaded-image';
    var IMAGES_SLIDESHOW = '.js-image-slideshow';

    function init() {

        var imagerImages = utilsHelper.toArray(document.querySelectorAll(IMAGE_LOADED_IMAGE));

        if (document.querySelectorAll(IMAGES_EVENTS).length) {
            new Imager(IMAGES_EVENTS, {
                availableWidths: guardian.membership.eventImages.widths,
                availablePixelRatios: guardian.membership.eventImages.ratios,
                lazyload: true,
                lazyloadOffset: 100
            });
        }

        if (imagerImages.length) {
            imagerImages.map(function(image) {
                var imageAvailableWidths = image.getAttribute('data-available-widths');
                new Imager([image], {
                    availableWidths: imageAvailableWidths ? imageAvailableWidths.split(',') : [],
                    lazyload: true,
                    lazyloadOffset: 100
                });
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

