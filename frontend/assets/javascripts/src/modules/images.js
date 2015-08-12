define(['respimage', 'lazySizes'], function(respimage, lazySizes) {
    'use strict';

    var LAZYLOAD_CLASS = 'js-lazyload';

    function init() {
        window.lazySizesConfig.lazyClass = LAZYLOAD_CLASS;
        var lazyImgs = document.querySelectorAll('.' + LAZYLOAD_CLASS);
        if(lazyImgs.length) {
            lazySizes.init();
        }
    }

    return {
        init: init
    };

});

