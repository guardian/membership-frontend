define(function() {

    var EXPERIMENT_ID = 3089970060;
    var VARIATION_INDEX = 1;

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
        /**
         * Run some extras steps if a user is in a specific A/B test
         * bucket/variation for an experiment.
         */
        if(typeof window.optimizely !== 'undefined') {
            var userVariations = window.optimizely.variationMap || false;
            if (userVariations && userVariations[EXPERIMENT_ID] === VARIATION_INDEX) {
                variantSteps();
            }
        }
    }

    return {
        init: init
    };

});
