define(function () {

    /*
     Cookie functions originally from http://www.quirksmode.org/js/cookies.html
     These are secure cookies and the value is JSON.stringify on set and JSON.parse on get
     */
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

        doc.cookie = [name, '=', JSON.stringify(value), expires, '; path=/', '; secure' ].join('');
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
                var value;

                try {
                    value = JSON.parse(c.substring(nameEQ.length, c.length));
                } catch(e){}

                return value;
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
