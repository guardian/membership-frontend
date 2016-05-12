/*global escape*/
define(['src/utils/atob','src/modules/raven'], function (AtoB,raven) {
    'use strict';

    return function(str) {
        /**
         * Wrap in try/catch because AtoB will return a fatal error if we try to decode a non-base64 value
         * Global escape: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/escape
         */
        var decoded;
        try {
            decoded = decodeURIComponent(escape(new AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
        } catch(e){
            raven.Raven.captureException(e, {tags: { level: 'info' }});
        }
        return decoded;
    };

});
