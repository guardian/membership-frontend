define(function(){

    var getUserOrSignIn = function(returnUrl){
        var user = getUserFromCookie();
        if (user) {
            return user;
        } else {
            window.location.href = '/signin?returnUrl=' + document.location.href;
        }
    };

    var getUserFromCookie = function(){
        var userFromCookieCache = null;

        function readCookie(name){
            var nameEQ = name + "=";
            var ca = document.cookie.split(';');
            for (var i = 0; i < ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0) == ' ') c = c.substring(1, c.length);
                if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
            }
            return null;
        }

        function decodeBase64(str){
            return decodeURIComponent(escape(AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
        }

        function AtoB(){
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
                    if (input.length % 4 === 1) throw INVALID_CHARACTER_ERR;
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

        if (userFromCookieCache === null) {
            var cookieData = readCookie('GU_U'),
                userData = cookieData ? JSON.parse(decodeBase64(cookieData.split('.')[0])) : null;
            if (userData) {
                userFromCookieCache = {
                    id: userData[0],
                    primaryemailaddress: userData[1], // this and siplayname are non camelcase to match with formstack
                    displayname: userData[2],
                    accountCreatedDate: userData[6],
                    emailVerified: userData[7],
                    rawResponse: cookieData
                };
                self.userFromCookieCache = userFromCookieCache;
            }
        }

        return userFromCookieCache;
    };

    return {
        getUserOrSignIn: getUserOrSignIn,
        getUserFromCookie: getUserFromCookie
    };
});