define([
    'bean',
    '$',
    'src/utils/analytics/ga'
], function (bean, $, googleAnalytics) {

    var filterInput  = document.getElementById('js-filter'),
        filterParent = document.getElementById('js-filter-container'),
        filterClear = $('.js-filter-clear'),
        filterCount  = $('.js-filter-count'),
        filterField  = filterInput.getAttribute('data-filter-field'),
        throttle     = 300, // how many milliseconds should we wait for typing to pause?
        currentTimeout;

    // create an index mapping any filter "key"
    // (eg. title, price) to DOM elements
    var index = $('.js-filter-item').map(function (item) {
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

    // filter the list based on the index
    var filterList = function () {
        if (currentTimeout) { window.clearTimeout(currentTimeout); }

        if (filterInput.value) {
            filterClear.removeClass('is-hidden');
        } else {
            filterClear.addClass('is-hidden');
        }

        // start search when the user pauses typing
        currentTimeout = window.setTimeout(function () {

            // fake a scroll event so lazy-load images appear
            bean.fire(document.body, 'scroll');

            var value = filterInput.value.toLowerCase();
            var elmsToShow = [],
                elmsToHide = [];

            if (value) {
                googleAnalytics.trackEvent('Event filter', 'Masterclasses', value);
            }

            index.forEach(function (item) {
                // use simple substring matching for now...
                var isFound = item.filters[filterField].toLowerCase().search(value);
                if (isFound !== -1) {
                    elmsToShow.push(item.elm);
                } else {
                    elmsToHide.push(item.elm);
                }
            });

            // remove the non-matching elements from the DOM
            $(elmsToHide).detach();

            // show matching elements
            // (which may have been hidden in previous searches)
            $(elmsToShow).appendTo(filterParent);

            // if no results, we show a message
            if (!elmsToShow.length) {
                $(filterParent).addClass('events-list--empty');
            } else {
                $(filterParent).removeClass('events-list--empty');
            }

            filterCount.text(elmsToShow.length);

        }, throttle);

    };

    // bind to typing in the search box
    bean.on(filterInput, 'keyup', filterList);

    var filterCategory = document.querySelector('.js-filter-category');
    bean.on(filterCategory, 'change', function () {
        var category = filterCategory.options[filterCategory.selectedIndex].value;
        window.location.href = '/masterclasses/' + category;
    });

    bean.on(filterClear[0], 'click', function (e) {
        e.preventDefault();

        filterInput.value = '';
        filterInput.focus();
        filterList();
    });

});
