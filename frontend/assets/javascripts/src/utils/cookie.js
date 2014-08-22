define(function () {

    // Cookie functions from http://www.quirksmode.org/js/cookies.html
    var doc = document;

    function setCookie(name, value, days) {
        var date;
        var expires;

        if (days) {
            date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toGMTString();
        } else {
            expires = '';
        }

        doc.cookie = [name, '=', value, expires, '; path=/'].join('');
    }

    function getCookie(name) {
        var nameEQ = name + '=';
        var ca = doc.cookie.split(';');
        var c;

        for (var i = 0; i < ca.length; i++) {
            c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1, c.length);
            }
            if (c.indexOf(nameEQ) === 0) {
                return c.substring(nameEQ.length, c.length);
            }
        }
        return null;
    }

    function removeCookie(name) {
        setCookie(name, '', -1);
    }

    return {
        setCookie: setCookie,
        getCookie: getCookie,
        removeCookie: removeCookie
    };
});
