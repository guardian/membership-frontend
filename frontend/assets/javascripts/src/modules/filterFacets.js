define(['lodash/forEach'], function(forEach) {
    'use strict';

    var FILTER_FACETS = '.js-facet';
    var FILTER_CATEGORIES = '.js-facet-category';

    function changeHandler(selector, callback) {
        var elems = document.querySelectorAll(selector);
        if(elems.length) {
            forEach(elems, function (elem) {
                elem.addEventListener('change', function(evt) {
                    callback.call(null, elem, evt);
                });
            });
        }
    }

    function init() {
        changeHandler(FILTER_FACETS, function(el) {
            var form = el.form;
            if(form) {
                form.submit();
            }
        });

        /**
         * Masterclasses category filter
         * TODO: Generalise to use generic submitOnChange method
         */
        changeHandler(FILTER_CATEGORIES, function(el) {
            var url = el.options[el.selectedIndex].value;
            window.location.href = url;
        });
    }

    return {
        init: init
    };

});
