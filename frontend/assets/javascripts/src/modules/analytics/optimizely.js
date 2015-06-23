define(function() {

    function variantSteps() {
        var els = [
            document.getElementById('qa-nav-about'),
            document.getElementById('qa-nav-pricing'),
            document.querySelector('.js-header-join-us-cta')
        ];
        els.forEach(function(el) {
            if(!el) { return; }
            el.className += ' u-h';
        });
    }

    function init() {
        /**
         * TODO: Need final variant ID and experiment ID
         */
        var experimentId = '12345';
        var variantId = '123456';
        var userVariations = window.optimizely.data.state.variationMap || false;
        if (userVariations && userVariations[experimentId] === variantId) {
            variantSteps();
        }
    }

    return {
        init: init
    };

});
