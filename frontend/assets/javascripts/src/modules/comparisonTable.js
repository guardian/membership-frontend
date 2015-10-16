define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var COMPARISON_TABLE_SELECTOR = '.comparison-table';
    var COMPARISON_TABLE = document.querySelector(COMPARISON_TABLE_SELECTOR);

    var HOVER_CLASS = 'is-hover';
    var ACTIVE_CLASS = 'is-active';
    var HOVERED = COMPARISON_TABLE_SELECTOR + ' .is-hover';
    var CLICKABLE = COMPARISON_TABLE_SELECTOR + ' .js-clickable';

    function init() {
        if (!COMPARISON_TABLE) {
            return;
        }

        addHoverListeners();
        addClickListeners();
    }

    function tierSelector(tier) {
        return '[data-tier="'+tier+'"]';
    }

    // We have to do this in JavaScript rather than using a CSS :hover class
    // because we have no single container which holds a tier column.
    // Instead they are split between rows, so we use data-tier to group them together.
    function addHoverListeners() {
        bean.on(COMPARISON_TABLE, 'mouseenter', CLICKABLE, function(e) {
            var tier = $(e.currentTarget).data('tier');
            var $elemsInThisTier = $(CLICKABLE + tierSelector(tier));

            $elemsInThisTier.addClass(HOVER_CLASS);
        });

        bean.on(COMPARISON_TABLE, 'mouseleave', CLICKABLE, function() {
            $(HOVERED).removeClass(HOVER_CLASS);
        });
    }

    function addClickListeners() {
        bean.on(COMPARISON_TABLE, 'click', CLICKABLE, function(e) {
            var tier = $(e.currentTarget).data('tier');
            var $elemsInThisTier = $(CLICKABLE + tierSelector(tier));
            var $elemsInOtherTiers = $(CLICKABLE+':not('+tierSelector(tier)+')');

            $elemsInThisTier.addClass(ACTIVE_CLASS);
            $elemsInOtherTiers.removeClass(ACTIVE_CLASS);
        });
    }

    return {
        init: init
    };
});
