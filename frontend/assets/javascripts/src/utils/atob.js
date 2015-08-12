/*eslint-disable */
define(function () {
    'use strict';

    return function AtoB(){
        return window.atob ? function(str){
            return window.atob(str);
        } : (function(){
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=',
                INVALID_CHARACTER_ERR = (function(){
                    // fabricate a suitable error object
                    try {
                        document.createElement('$');
                    }
                    catch (error) {
                        return error;
                    }
                }());

            return function(input){
                input = input.replace(/[=]+$/, '');
                if (input.length % 4 === 1) {
                    throw INVALID_CHARACTER_ERR;
                }
                for (
                    var bc = 0, bs, buffer, idx = 0, output = '';
                    buffer = input.charAt(idx++);
                    ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer,
                        bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0
                    ) {
                    buffer = chars.indexOf(buffer);
                }
                return output;
            };
        })();
    }
});
/*eslint-enable */
