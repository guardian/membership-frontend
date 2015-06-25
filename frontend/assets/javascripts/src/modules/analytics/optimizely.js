define(function() {

    var EXPERIMENT_ID = 3089970060;

    function variantSteps() {
        var HIDDEN_CLASS = 'u-h';
        var ELS = [
            document.getElementById('qa-nav-about'),
            document.getElementById('qa-nav-pricing'),
            document.querySelector('.js-header-join-us-cta')
        ];
        ELS.forEach(function(el) {
            if(!el) { return; }
            el.className += ' ' + HIDDEN_CLASS;
        });
    }

    function init() {
        var userVariations = window.optimizely.variationMap || false;
        if (userVariations && userVariations[EXPERIMENT_ID] === 1) {
            variantSteps();
        }
    }

    return {
        init: init
    };

});
