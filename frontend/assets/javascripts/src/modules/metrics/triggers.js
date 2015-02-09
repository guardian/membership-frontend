define([
    'lodash/function/debounce',
    'src/modules/metrics/logger'
], function (debounce, logMetric) {

    var METRIC_ATTRS = {
        'trigger': 'data-metric-trigger',
        'category': 'data-metric-category',
        'action': 'data-metric-action',
        'label': 'data-metric-label' // Optional
    };

    function init() {
        var trackingElems = document.querySelectorAll('[' + METRIC_ATTRS.trigger + ']');
        if (window.ga && trackingElems) {
            configureListeners(trackingElems);
        }
    }

    function configureListeners(trackingElems) {
        var TRACKING_EVENTS = {
            'click' : clickListener,
            'keyup' : keyupListener
        };
        [].forEach.call(trackingElems, function(elem) {
            var key = elem.getAttribute(METRIC_ATTRS.trigger);
            var metricEvent = TRACKING_EVENTS[key];
            if (metricEvent) {
                metricEvent.call(this, elem);
            }
        });
    }

    function extractMetricsFromListener(elem, label) {
        var category = elem.getAttribute(METRIC_ATTRS.category);
        var action = elem.getAttribute(METRIC_ATTRS.action);
        if (category && action) {
            logMetric({
                'eventCategory': category,
                'eventAction': action,
                'eventLabel': label || false // Label is optional
            });
        }
    }

    function getLabel(elem) {
        var label = elem.getAttribute(METRIC_ATTRS.label) || elem.textContent;
        label += ' | ' + window.location.pathname;
        return label;
    }

    function clickListener(elem) {
        elem.addEventListener('click', function() {
            extractMetricsFromListener(elem, getLabel(elem));
        });
    }

    function keyupListener(elem) {
        elem.addEventListener('keyup', debounce(function() {
            extractMetricsFromListener(elem, elem.value.toLowerCase());
        }, 500));
    }

    return {
        init: init
    };
});
