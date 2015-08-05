define(function() {

    var EXPERIMENT_ID = 3267360204;
    var VARIATION_INDEX = 1;

    function updateHomeLink() {
        var homeLink = document.querySelector('.js-nav-link-home');
        if(homeLink) {
            homeLink.setAttribute('href', '/join-challenger');
        }
    }

    function hideNavItems() {
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

    function variantSteps() {
        updateHomeLink();
        hideNavItems();
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
