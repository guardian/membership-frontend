define([
    'bean',
    'src/modules/metrics/logger',
    'src/modules/form/helper/formUtil'
], function (bean, logMetric, formUtil) {

    /**
     * Successful submission: Form was submitted without errors
     *
     * Sends a single success events
     */
    function recordFormSuccess(formAction) {
        logMetric({
            'eventCategory': 'form',
            'eventAction': 'submit:success',
            'eventLabel': formAction
        });
    }

    /**
     * Failed submission: Form was submitted but has errors
     *
     * Sends an error event with a count of the number of errors and one event per field that has errors
     */
    function recordFormWithErrors(formAction) {
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
    }

    /**
     * Abandoned form: Form was left/not-submitted
     *
     * Sends an abandoned event and one event per empty field (for required fields only)
     */
    function recordAbandonedForm(formAction) {
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
    }

    function init() {
        var formAction;
        if (window.ga && formUtil) {
            formAction = formUtil.elem.getAttribute('action');
            bean.on(window, 'beforeunload.formMetrics', function() {
                recordAbandonedForm(formAction);
            });
            bean.on(formUtil.elem.querySelector('[type="submit"]'), 'click', function() {
                // Unbind `beforeunload` listener as form submission counts as `beforeunload`
                bean.off(window, 'beforeunload.formMetrics');
                if (formUtil.errs.length) {
                    recordFormWithErrors(formAction);
                } else {
                    recordFormSuccess(formAction);
                }
            });
        }
    }

    return {
        init: init
    };
});
