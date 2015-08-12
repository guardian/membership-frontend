define(function() {
    'use strict';

    function load() {
        require(['js!//script.crazyegg.com/pages/scripts/0030/6248.js?' + Math.floor(new Date().getTime() / 3600000)]);
    }

    return {
        load: load
    };
});
