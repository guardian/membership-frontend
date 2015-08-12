define(['src/utils/atob'], function (AtoB) {
    'use strict';

    return function(str) {
        /* global escape: true */
        /* See: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/escape */
        /**
         * Wrap in try/catch because AtoB will return a fatal error if we try to decode a non-base64 value
         */
        var decoded;
        try {
            decoded = decodeURIComponent(escape(new AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
        } catch(e){}
        return decoded;
    };

});
