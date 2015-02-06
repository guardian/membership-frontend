/* global ga */
define([
    'lodash/object/extend',
    'lodash/function/debounce',
    'src/utils/url'
], function (extend, debounce, urlHelper) {

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

    function logMetric(options) {
        var event = false;
        if (window.ga) {
            event = ga('send', extend({
                'hitType': 'event'
            }, options));
        }
        return event;
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

    function extractMetricsFromListener(elem, label, extras) {

        var category = elem.getAttribute(METRIC_ATTRS.category);
        var action = elem.getAttribute(METRIC_ATTRS.action);

        if (category && action) {
            logMetric(extend({
                'eventCategory': category,
                'eventAction': action,
                'eventLabel': label || false // Label is optional
            }, extras));
        }
    }

    function clickListener(elem) {
        elem.addEventListener('click', function(e) {
            var url, label, extra;

            url = elem.getAttribute('href');
            label = elem.getAttribute(METRIC_ATTRS.label) || elem.textContent;
            label += ' | ' + window.location.pathname;

            if (urlHelper.isExternal(url)) {
                e.preventDefault();
                extra = {
                    'hitCallback': function () {
                        document.location = url;
                    }
                };
            }
            extractMetricsFromListener(elem, label, extra);
        });
    }

    function keyupListener(elem) {
        elem.addEventListener('keyup', debounce(function() {
            var label = elem.value.toLowerCase();
            extractMetricsFromListener(elem, label);
        }, 500));
    }

    return {
        init: init,
        logMetric: logMetric
    };
});
