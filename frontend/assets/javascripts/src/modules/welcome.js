define(['src/utils/user'], function(userUtil) {
    'use strict';

    var urlMappings = {
        '/': '/welcome'
    };

    function init() {
        var redirectUrl = urlMappings[window.location.pathname] || false;
        if (redirectUrl) {
            userUtil.getMemberDetail(function (memberDetail, hasTier) {
                if (hasTier) {
                    window.location.href = redirectUrl;
                }
            });
        }
    }

    return {
        init: init
    };
});
