import * as bean from 'bean'
import $ from '$'

'use strict';

const COMPARISON_TABLE_SELECTOR = '.comparison-table';
const COMPARISON_TABLE = document.querySelector(COMPARISON_TABLE_SELECTOR);

const HOVER_CLASS = 'is-hover';
const ACTIVE_CLASS = 'is-active';
const HOVERED = COMPARISON_TABLE_SELECTOR + ' .is-hover';
const CLICKABLE = COMPARISON_TABLE_SELECTOR + ' .js-clickable';

export function init() {
    if (!COMPARISON_TABLE) {
        return;
    }

    addHoverListeners();
}

function tierSelector(tier) {
    return '[data-tier="' + tier + '"]';
}

// We have to do this in JavaScript rather than using a CSS :hover class
// because we have no single container which holds a tier column.
// Instead they are split between rows, so we use data-tier to group them together.
function addHoverListeners() {
    bean.on(COMPARISON_TABLE, 'mouseenter', CLICKABLE, e => {
        let tier = $(e.currentTarget).data('tier');
        let $elemsInThisTier = $(CLICKABLE + tierSelector(tier));
        let $elemsInOtherTiers = $(CLICKABLE + ':not(' + tierSelector(tier) + ')');

        $elemsInThisTier.addClass(HOVER_CLASS);
        $elemsInThisTier.addClass(ACTIVE_CLASS);
        $elemsInOtherTiers.removeClass(ACTIVE_CLASS);
    });

    bean.on(COMPARISON_TABLE, 'mouseleave', CLICKABLE, () => {
        $(HOVERED).removeClass(HOVER_CLASS);
    });
}



