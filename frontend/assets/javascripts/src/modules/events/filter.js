define(['$'], function ($) {

    var FILTER_INPUT    = document.getElementById('js-filter');
    var FILTER_PARENT   = document.getElementById('js-filter-container');
    var FILTER_CATEGORY = $('.js-filter-category');
    var FILTER_ITEMS    = $('.js-filter-item');
    var FILTER_CLEAR    = $('.js-filter-clear');
    var FILTER_COUNT    = $('.js-filter-count');
    var FILTER_EMPTY    = $('.js-filter-empty');
    var THROTTLE        = 300;
    var HIDDEN_CLASS    = 'is-hidden';
    var SHOWN_CLASS     = 'is-shown';

    var currentTimeout;

    // Create an index mapping any filter "key"
    // (eg. title, price) to DOM elements
    var index = FILTER_ITEMS.map(function (item) {
        var filters = {};
        $('[data-filter-key]', item).each(function (f) {
            var elm = $(f);
            filters[elm.data('filter-key')] = elm.text();
        });
        return {
            elm: item,
            filters: filters
        };
    });

    function filterList() {

        if (currentTimeout) { window.clearTimeout(currentTimeout); }

        // Toggle clear filter button
        toggleClear(FILTER_INPUT.value);

        // Start search when the user pauses typing
        currentTimeout = window.setTimeout(function () {

            var value = FILTER_INPUT.value.toLowerCase();

            // Build results object
            var results = buildResults(value);

            // Remove the non-matching elements from the DOM
            $(results.hide).detach();

            // Append matching elements to the DOM
            $(results.show).appendTo(FILTER_PARENT);

            // Fake a scroll event so lazy-load images appear
            triggerScroll();

            // Handle no results case
            handleNoResults(results.show.length);

            // Update result count
            FILTER_COUNT.text(results.show.length);

        }, THROTTLE);

    }

    function triggerScroll() {
        // Trigger native scroll event
        var event = document.createEvent('HTMLEvents');
        event.initEvent('scroll', true, true);
        window.dispatchEvent(event);
    }

    function buildResults(value) {
        var results = {
            show: [],
            hide: []
        };
        index.forEach(function (item) {
            // use simple substring matching for now...
            var isFound = item.filters[FILTER_INPUT.getAttribute('data-filter-field')].toLowerCase().search(value);
            if (isFound !== -1) {
                results.show.push(item.elm);
            } else {
                results.hide.push(item.elm);
            }
        });
        return results;
    }

    function toggleClear(val) {
        if (val) {
            FILTER_CLEAR.removeClass(HIDDEN_CLASS);
        } else {
            FILTER_CLEAR.addClass(HIDDEN_CLASS);
        }
    }

    function handleNoResults(elemCount) {
        if (!elemCount) {
            FILTER_EMPTY.addClass(SHOWN_CLASS);
        } else {
            FILTER_EMPTY.removeClass(SHOWN_CLASS);
        }
    }

    function init() {
        if(FILTER_INPUT) {
            FILTER_INPUT.addEventListener('keyup', filterList);
            FILTER_CATEGORY.each(function (elem) {
                elem.addEventListener('change', function() {
                    var url = elem.options[elem.selectedIndex].value;
                    window.location.href = url;
                });
            });
            FILTER_CLEAR[0].addEventListener('click', function(event) {
                event.preventDefault();
                FILTER_INPUT.value = '';
                FILTER_INPUT.focus();
                filterList();
            });
        }
    }

    return {
        init: init
    };

});
