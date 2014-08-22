define([
    'src/utils/storage',
    'src/utils/atob',
    'ajax'
], function(storage, AtoB, ajax){

    var MEM_USER_STORAGE_KEY = 'memUser';

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
        var store = storage.local;
        var membershipUser = store.get(MEM_USER_STORAGE_KEY);
        var identityUser = getUserFromCookie();
        var today = new Date();
        var todayPlus24Hours = today.setDate(today.getDate() + 1);

        if (identityUser) {
            if ((membershipUser && membershipUser.userId) === identityUser.id) {
                callback(membershipUser);
            } else {
                ajax({
                    url: '/user/me',
                    method: 'get',
                    success: function (resp) {
                        callback(resp);
                        store.set(MEM_USER_STORAGE_KEY, resp, todayPlus24Hours)
                    },
                    error: function (err) {
                        callback(null, err);
                        /* we get a 403 error for guardian users who are not members id added to stop this from
                        being called on each page */
                        store.set(MEM_USER_STORAGE_KEY, { userId: identityUser.id }, todayPlus24Hours)
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
