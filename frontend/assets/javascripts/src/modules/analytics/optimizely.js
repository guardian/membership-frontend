define(function() {

    var SETTINGS = {
        experimentId: 3089970060,
        conrolId: 3084500052,
        variationId: 3091240050
    };

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
        var userVariations = window.optimizely.data.state.variationMap || false;
        if (userVariations && userVariations[SETTINGS.experimentId] === SETTINGS.variationId) {
            variantSteps();
        }
    }

    return {
        init: init
    };

});
