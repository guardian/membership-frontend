define([
    'config/appCredentials',
    'src/utils/cookie',
    'src/utils/user'
], function (appCredentials, cookie, userUtil) {

    /**
     * The following property has recently been added to the /user/me endpoint.
     * Some existing logged in users will have stored cookies without this property.
     * The following check removes the stored cookie, regular application logic
     * thereafter will recheck the endpoint.
     */
    var requiredProperty = 'firstName';

    function check() {
        userUtil.getMemberDetail(function (memberDetail) {
            if (memberDetail) {
                if (!memberDetail[requiredProperty]) {
                    cookie.removeCookie(appCredentials.membership.userCookieKey);
                }
            }
        });
    }

    return {
        init: check
    };
});
