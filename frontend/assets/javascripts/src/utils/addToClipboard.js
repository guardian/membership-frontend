define(function () {
    'use strict';

    function init () {

        var copyToClipBoardElem = document.querySelectorAll('.js-copy-to-clipboard-button');

        if (copyToClipBoardElem.length) {
            require(['zeroclipboard'], function (ZeroClipboard) {
                new ZeroClipboard(copyToClipBoardElem);
            });
        }
    }

    return {
        init: init
    };
});
