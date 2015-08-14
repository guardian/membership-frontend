define(function() {
    'use strict';

    return {
        trimWhitespace: function(str) {
            return str.replace(/(^\s+|\s+$)/g, '');
        },
        removeWhitespace: function(str) {
            return str.replace(/\s+/g, '');
        }
    };
});
