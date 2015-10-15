/**
 * This file:
 * Controls the listeners for the identity icon menu found in the header
 * Controls the html document listener to close the identity icon menu
 * Sets the identity icon returnUrl when a user needs to sign in (controlled via JavaScript for caching reasons)
 */
define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var HOVER_CLASS = 'is-hover';
    var ACTIVE_CLASS = 'is-active';
    var COMPARISON_TABLE = document.querySelector('.comparison-table');

    function init() {
        addHoverListeners();
        addClickListeners();
    }

    function addClassToTier(clazz, tier) {
        var tierElems = COMPARISON_TABLE.querySelectorAll('.js-clickable[data-tier="'+tier+'"]');
        for (var i = 0; i < tierElems.length; i++) {
            tierElems[i].classList.add(clazz);
        }
    }

    function addHoverListeners() {
        $('.js-clickable').each(function(elem) {
            bean.on(elem, 'mouseenter', function(e) {
                var elem = e.srcElement;
                var tier = $(elem).data('tier');
                while (!tier) {
                    elem = elem.parentElement;
                    tier = $(elem).data('tier');
                }

                addClassToTier(HOVER_CLASS, tier);
            });
            bean.on(elem, 'mouseleave', function() {
                var hoveredElems = COMPARISON_TABLE.querySelectorAll('.is-hover');
                for (var i = 0; i < hoveredElems.length; i++) {
                    hoveredElems[i].classList.remove('is-hover');
                }
            });
        });
    }

    function addClickListeners() {
        bean.on(COMPARISON_TABLE, 'click', '.js-clickable', function(e) {
            var tier = $(e.currentTarget).data('tier');
            addClassToTier(ACTIVE_CLASS, tier);

            var otherElems = COMPARISON_TABLE.querySelectorAll('.js-clickable:not([data-tier="'+tier+'"])');
            for (var i = 0; i < otherElems.length; i++) {
                otherElems[i].classList.remove(ACTIVE_CLASS);
            }
        });
    }

    return {
        init: init
    };
});
