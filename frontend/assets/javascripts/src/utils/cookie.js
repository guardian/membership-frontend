define(['src/utils/decodeBase64'], function (decodeBase64) {

    /*
     Cookie functions originally from http://www.quirksmode.org/js/cookies.html
     */
    function setCookie(name, value, days, isUnSecure) {
        var date;
        var expires;
        // used for testing purposes, cookies are secure by default
        var secureCookieString = isUnSecure ? '' : '; secure';

        if (days) {
            date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toGMTString();
        } else {
            expires = '';
        }

        document.cookie = [name, '=', value, expires, '; path=/', secureCookieString ].join('');
    }

    function getCookie(name) {
        var nameEQ = name + '=';
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ') { c = c.substring(1, c.length); }
            if (c.indexOf(nameEQ) === 0) { return c.substring(nameEQ.length, c.length); }
        }
        return null;
    }

    function getDecodedCookie(name) {
        return decodeCookie(getCookie(name));
    }

    function removeCookie(name) {
        setCookie(name, '', -1);
    }

    function decodeCookie(cookieData) {
        /**
         * Check to see if we have cookie data AND is base64
         */
        var cookieVal = cookieData ? decodeBase64(cookieData.split('.')[0]) : undefined;
        return cookieVal ? JSON.parse(cookieVal) : undefined;
    }

    return {
        setCookie: setCookie,
        getCookie: getCookie,
        getDecodedCookie: getDecodedCookie,
        removeCookie: removeCookie,
        decodeCookie: decodeCookie
    };

});
