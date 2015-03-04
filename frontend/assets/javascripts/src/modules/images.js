define([
    'respimage',
    'lazySizes',
    'lib/bower-components/imager.js/Imager',
    'src/utils/helper'
], function(respimage, lazySizes, Imager, utilsHelper) {

    var LAZYLOAD_CLASS = 'js-lazyload';
    var IMAGE_LOADED_IMAGE = '.js-imager-loaded-image';
    var IMAGES_SLIDESHOW = '.js-image-slideshow';

    function init() {

        window.lazySizesConfig.lazyClass = LAZYLOAD_CLASS;
        var lazyImgs = document.querySelectorAll('.' + LAZYLOAD_CLASS);
        if(lazyImgs.length) {
            lazySizes.init();
        }

        var imagerImages = utilsHelper.toArray(document.querySelectorAll(IMAGE_LOADED_IMAGE));

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

