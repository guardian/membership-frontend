define([
    'bean',
    'src/modules/metrics/logger',
    'src/modules/form/helper/formUtil'
], function (bean, logMetric, formUtil) {

    function init() {
        if (window.ga && formUtil) {
            trackForms();
        }
    }

    function trackForms() {

        var formAction = formUtil.elem.getAttribute('action');
        var formSubmit = formUtil.elem.querySelector('[type="submit"]');

        bean.on(window, 'beforeunload.formMetrics', function() {

            logMetric({
                'eventCategory': 'form',
                'eventAction': 'abandoned',
                'eventLabel': formAction,
                'eventValue': formUtil.errs.length
            });

            formUtil.errs.forEach(function(er) {
                logMetric({
                    'eventCategory': 'form',
                    'eventAction': 'abandoned:error',
                    'eventLabel': er + ' | ' + formAction
                });
            });

            formUtil.elems
                .filter(function(el) { return !el.value; })
                .map(function(el) {
                    logMetric({
                        'eventCategory': 'form',
                        'eventAction': 'abandoned:empty',
                        'eventLabel': (el.name || el.id) + ' | ' + formAction
                    });
                });

        });

        bean.on(formSubmit, 'click', function() {

            bean.off(window, 'beforeunload.formMetrics');

            if (formUtil.errs.length) {
                logMetric({
                    'eventCategory': 'form',
                    'eventAction': 'submit:failure',
                    'eventLabel': formAction,
                    'eventValue': formUtil.errs.length
                });

                formUtil.errs.forEach(function(er) {
                    logMetric({
                        'eventCategory': 'form',
                        'eventAction': 'submit:error',
                        'eventLabel': er + ' | ' + formAction
                    });
                });
            } else {
                logMetric({
                    'eventCategory': 'form',
                    'eventAction': 'submit:success',
                    'eventLabel': formAction
                });
            }
        });

    }

    return {
        init: init
    };
});
