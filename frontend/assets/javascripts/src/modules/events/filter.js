/*global ga */
define(['string_score', 'bean', '$'], function (string_score, bean, $) {

    var filterInput  = document.getElementById('js-filter'),
        filterParent = document.getElementById('js-filter-container'),
        filterField  = filterInput.getAttribute('data-filter-field'),
        minScore     = 0.3, // what score cutoff should searches use?
        fuzziness    = 0.5, // how generous should the search match be?
        throttle     = 200, // how many milliseconds should we wait for typing to pause?
        currentTimeout;

    // track what people filter on
    var trackSearch = function (category, action, label) {
        ga('send', 'event', category, action, label);
    };

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
    // and re-order elements by score
    var filterList = function (e) {
        e.preventDefault();

        if (currentTimeout) { window.clearTimeout(currentTimeout); }

        // start search when the user pauses typing
        currentTimeout = window.setTimeout(function () {

            var value = filterInput.value;
            var elmsToShow = [],
                elmsToHide = [];

            if (value) {
                trackSearch('Event filter', 'Typed search', value);
            }

            index.forEach(function (item) {
                var score = item.filters[filterField].score(value, fuzziness);
                if (!value || score > minScore) {
                    elmsToShow.push(item.elm);
                    // track the score so we can sort on it later
                    $(item.elm).data('score', score);
                } else {
                    elmsToHide.push(item.elm);
                }
            });

            // remove the non-matching elements from the DOM
            $(elmsToHide).detach();

            // sort by score
            elmsToShow.sort(function (a, b) {
                var scoreA = parseFloat($(a).data('score')),
                    scoreB = parseFloat($(b).data('score'));
                return scoreA < scoreB;
            });

            // re-add the sorted elements to the DOM
            $(elmsToShow).appendTo(filterParent);

            // if no results, we show a message
            if (!elmsToShow.length) {
                $(filterParent).addClass('events-list--empty');
            } else {
                $(filterParent).removeClass('events-list--empty');
            }

        }, throttle);

    };

    // bind to typing in the search box
    bean.on(filterInput, 'keyup', filterList);

});
