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

    function iconGrid() {
        var SVG_EMBED = document.getElementById('svg-sprite');
        var ICON_GRID = document.querySelectorAll('.js-icon-grid');
        [].forEach.call(ICON_GRID, function(grid) {
            var iconList = [].map.call(SVG_EMBED.querySelectorAll('symbol'), function(symbol) {
                return symbol.id;
            });
            var html = iconList.map(function(id) {
                return '<svg class="icon-inline"><use xlink:href="#' + id + '"/></use></svg>';
            }).join('');
            grid.innerHTML = html;
        });
    }

    function init() {

        var patterns = buildItems('.js-pattern-item'),
            select;

        if (patterns.length) {
            select = buildSelect(patterns);
            renderPatternNav('.js-pattern-nav', select);
            bindNavEvents('.js-pattern-selector');
            iconGrid();
        }

    }

    return {
        init: init
    };

});
