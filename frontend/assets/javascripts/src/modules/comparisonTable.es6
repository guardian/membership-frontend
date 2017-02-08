import $ from '$'

'use strict';

const COMPARISON_TABLE_CLASS_NAME = 'comparison-table';
const COMPARISON_TABLE = document.getElementsByClassName(COMPARISON_TABLE_CLASS_NAME);

const HOVER_CLASS = 'is-hover';
const ACTIVE_CLASS = 'is-active';
const HOVERED = '.' + COMPARISON_TABLE_CLASS_NAME + ' .is-hover';
const CLICKABLE = '.' + COMPARISON_TABLE_CLASS_NAME + ' .js-clickable';

export function init() {
    if (COMPARISON_TABLE.length == 0) {
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
    let elems = document.querySelectorAll(CLICKABLE);
    for(var i = 0 ; i < elems.length ; i++){
        elems[i].addEventListener('mouseenter', e => {
            let tier = $(e.currentTarget).data('tier');
            let $elemsInThisTier = $(CLICKABLE + tierSelector(tier));
            let $elemsInOtherTiers = $(CLICKABLE + ':not(' + tierSelector(tier) + ')');

            $elemsInThisTier.addClass(HOVER_CLASS);
            $elemsInThisTier.addClass(ACTIVE_CLASS);
            $elemsInOtherTiers.removeClass(ACTIVE_CLASS);
        });

        elems[i].addEventListener('mouseleave', () => {
            $(HOVERED).removeClass(HOVER_CLASS);
        });
    }
}



