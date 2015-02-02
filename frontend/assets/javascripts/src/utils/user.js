define([
    'src/utils/atob',
    'ajax',
    'src/utils/cookie',
    'config/appCredentials'
], function(AtoB, ajax, cookie, appCredentials){

    var MEM_USER_COOKIE_KEY = appCredentials.membership.userCookieKey;

    var isLoggedIn = function(){
        return !!getUserFromCookie();
    };

    var idCookieAdapter = function (data, rawCookieString) {
        return {
            id: data[0],
            displayname: data[2],
            accountCreatedDate: data[6],
            emailVerified: data[7],
            rawResponse: rawCookieString
        };
    };

    var getUserFromCookie = function(){
        var cookieData = cookie.getCookie('GU_U');
        var userData = cookie.decodeCookie(cookieData);
        var userFromCookieCache;
        if (userData) {
            userFromCookieCache = idCookieAdapter(userData, cookieData);
        }
        return userFromCookieCache;
    };

    /**
     * get the membership user details.
     * This will call '/user/me' if a valid identity user is logged in and the membership cookie is not stored and the
     * membershipUserId does not match the identity userId,
     * If the identity member does not have a membership tier then the cookie is stored with the identity user id,
     * If the identity member does have a membership tier then the membership details are stored in the membershipUser
     * cookie.
     * If membership cookie exists and matches identity credentials then this is used over preference of calling
     * '/user/me'
     * @param callback
     */
    var getMemberDetail = (function () {
        var pendingXHR;
        var callbacks = [];
        var invokeCallbacks = function (args) {
            callbacks.map(function (callback) {
                callback.apply(this, args);
            });
        };

        return function (callback, overRideCallbacks) {
            // for testing purposes to mimic a reload of the javascript and hence a clearing of the callbacks array
            callbacks = overRideCallbacks || callbacks;

            var membershipUser;
            var identityUser = getUserFromCookie();

            if (identityUser) {
                membershipUser = cookie.getDecodedCookie(MEM_USER_COOKIE_KEY);
                if ((membershipUser && membershipUser.userId) === identityUser.id) {
                    callback(membershipUser);
                } else {
                    callbacks.push(callback);

                    if (!pendingXHR) {
                        pendingXHR = ajax({
                            url: '/user/me',
                            method: 'get',
                            success: function (resp) {
                                invokeCallbacks([resp]);
                                pendingXHR = null;
                            }
                        });
                    }
                }
            } else {
                callback(null);
            }
        };
    })();

    return {
        isLoggedIn: isLoggedIn,
        getUserFromCookie: getUserFromCookie,
        getMemberDetail: getMemberDetail,
        idCookieAdapter: idCookieAdapter
    };
});
