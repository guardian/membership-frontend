define(['src/utils/atob'], function (AtoB) {

    return function(str) {
        /*global escape: true */
        /* See: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/escape */
        return decodeURIComponent(escape(new AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
    };

});
