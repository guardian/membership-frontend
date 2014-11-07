define(['string_score', 'bean', '$'], function (string_score, bean, $) {

    var filterInput = document.getElementById('js-filter');
    var filterField = filterInput.getAttribute('data-filter-field');
    var minScore = 0.3; // how accurate should searches be?
    var fuzziness = 0.5; // how generous is the match

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

    // todo: reorder elements by score
    bean.on(filterInput, 'keyup', function () {
        var value = this.value;
        var matchCount = 0;
        index.forEach(function (item) {
            if (!value || item.filters[filterField].score(value, fuzziness) > minScore) {
                $(item.elm).removeClass('event-item--pristine').show();
                matchCount++;
                if (matchCount % 3 -1 === 1) {
                    $(item.elm).addClass('event-item--filtered');
                } else {
                    $(item.elm).removeClass('event-item--filtered');
                }
            } else {
                $(item.elm).addClass('event-item--pristine').removeClass('event-item--filtered').hide();
            }
        });
    });

});