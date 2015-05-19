/*
 * basic tab module
 *
 */
var ACTIVE_TAB_CLASS    = 'tabs__tab--active';
var ACTIVE_PANE_CLASS   = 'tabs__content--active';
var PANE_DATA_ATTR      = 'data-tab-pane';
var TAB_HOLDER          = 'js-tabs';
var TAB_ITEM            = 'js-tabs__tab';

var tabSets = document.querySelectorAll('.' + TAB_HOLDER);
[].forEach.call(tabSets, function(tabSet) {
    var tabs = tabSet.querySelectorAll('.' + TAB_ITEM);
    var paneHolder = document.getElementById(tabSet.getAttribute(PANE_DATA_ATTR));

    [].forEach.call(tabs, function(tab) {
        tab.addEventListener('click', function(e) {

            var prevActiveTab = tabSet.querySelector('.' + ACTIVE_TAB_CLASS);
            if (prevActiveTab) { prevActiveTab.classList.remove(ACTIVE_TAB_CLASS); }

            var prevActivePane = paneHolder.querySelector('.' + ACTIVE_PANE_CLASS);
            if (prevActivePane) { prevActivePane.classList.remove(ACTIVE_PANE_CLASS); }

            tab.classList.toggle(ACTIVE_TAB_CLASS);
            var pane = tab.getAttribute('href').replace('#', '');
            if (pane) {
                document.getElementById(pane).classList.toggle(ACTIVE_PANE_CLASS);
            }

            e.preventDefault();
        });
    });
});


var SEARCH_FIELD     = 'js-search';
var SEARCHABLES      = 'js-searchable';
var SEARCH_DATA_ATTR = 'data-search';
var CLASS_TO_TOGGLE  = 'hidden';

var searchInput = document.querySelector('.' + SEARCH_FIELD);
var elements    = document.querySelectorAll('.' + SEARCHABLES);

searchInput.addEventListener('keyup', function() {
    var searchTerm = searchInput.value;
    [].forEach.call(elements, function(elm) {
        if (!searchTerm || searchTerm === '') {
            elm.classList.remove(CLASS_TO_TOGGLE);
        } else {
            var text = elm.getAttribute(SEARCH_DATA_ATTR);
            var isFound = text.toLowerCase().search(searchTerm);
            if (isFound !== -1) {
                elm.classList.remove(CLASS_TO_TOGGLE);
            } else {
                elm.classList.add(CLASS_TO_TOGGLE);
            }
        }
    });
});
