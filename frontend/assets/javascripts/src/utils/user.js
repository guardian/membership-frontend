define([
    'src/utils/atob',
    'ajax',
    'src/utils/cookie'
], function(AtoB, ajax, cookie){

    var MEM_USER_COOKIE_KEY = 'memUser';

    var isLoggedIn = function(){
        return !!getUserFromCookie();
    };

    var getUserFromCookie = function(){
        var userFromCookieCache = null;

        function readCookie(name){
            var nameEQ = name + '=';
            var ca = document.cookie.split(';');
            for (var i = 0; i < ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0) === ' ') { c = c.substring(1, c.length); }
                if (c.indexOf(nameEQ) === 0) { return c.substring(nameEQ.length, c.length); }
            }
            return null;
        }

        function decodeBase64(str){
            return decodeURIComponent(escape(new AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
        }

        if (userFromCookieCache === null) {
            var cookieData = readCookie('GU_U'),
                userData = cookieData ? JSON.parse(decodeBase64(cookieData.split('.')[0])) : null;
            if (userData) {
                userFromCookieCache = {
                    id: userData[0],
                    primaryemailaddress: userData[1], // this and displayname are non camelcase to match with formstack
                    displayname: userData[2],
                    accountCreatedDate: userData[6],
                    emailVerified: userData[7],
                    rawResponse: cookieData
                };
            }
        }

        return userFromCookieCache;
    };

    var getMemberDetail = function (callback) {
        var membershipUser = cookie.getCookie(MEM_USER_COOKIE_KEY);
        var identityUser = getUserFromCookie();

        if (identityUser) {
            if ((membershipUser && membershipUser.userId) === identityUser.id) {
                callback(membershipUser);
            } else {
                /* until cookies are destroyed kill cookies if they are around - this is the use case of logging in and
                   out with different users */
                cookie.removeCookie(MEM_USER_COOKIE_KEY);
                ajax({
                    url: '/user/me',
                    method: 'get',
                    success: function (resp) {
                        callback(resp);
                        cookie.setCookie(MEM_USER_COOKIE_KEY, resp);
                    },
                    error: function (err) {
                        callback(null, err);
                        /* we get a 403 error for guardian users who are not members id added to stop this from
                           being called on each page. We however do not want to set this cookie if there has been an
                           error when a user is a member */
                        if (!membershipUser) {
                            cookie.setCookie(MEM_USER_COOKIE_KEY, { userId: identityUser.id });
                        }
                    }
                });
            }
        }
    };

    return {
        isLoggedIn: isLoggedIn,
        getUserFromCookie: getUserFromCookie,
        getMemberDetail: getMemberDetail
    };
});
