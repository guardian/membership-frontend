define(function() {

    function buildItems(selector) {
        var items = [];
        [].forEach.call(document.querySelectorAll(selector), function(el, index) {
            el.id = 'pattern-' + index;
            var option = [
                '<option value="#pattern-' + index + '">',
                    el.getAttribute('data-pattern-label'),
                '</option>'
            ].join('');
            items.push( option );
        }, false);
        return items;
    }

    function buildSelect(items) {
        return [
            '<select class="js-pattern-selector">',
                '<option value="">Select a pattern</option>',
                items.join(''),
            '</select>'
        ].join('');
    }

    function renderPatternNav(selector, select) {
        document.querySelector(selector).innerHTML = select;
    }

    function bindNavEvents(selector) {
        var nav = document.querySelector(selector);
        if (nav) {
            nav.onchange = function() {
              var val = this.value;
              if (val) {
                window.location = val;
              }
            };
        }
    }

    function init() {

        var patterns = buildItems('.js-pattern-item'),
            select;

        if (patterns.length) {
            select = buildSelect(patterns);
            renderPatternNav('.js-pattern-nav', select);
            bindNavEvents('.js-pattern-selector');
        }

    }

    return {
        init: init
    };

});
