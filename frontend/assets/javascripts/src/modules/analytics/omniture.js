/*global Raven */
define([
    'src/utils/user'
], function (user) {

    var GU_ID_STRING = 'GUID';
    var NONE_STRING = 'none';
    var MEMBERSHIP_STRING = 'Membership';

    function init() {
        require('js!omniture').then(onSuccess, function(err) {
            Raven.captureException(err);
        });
    }

    function onSuccess() {
        /*global s_gi: true */
        var s = s_gi('guardiangu-network');
        var s_code;
        var identityUser = user.getUserFromCookie();
        var identityId = identityUser && identityUser.id;
        var referrerArray = document.referrer.split('/');
        var referrerDomain;

        if (referrerArray.length > 2) {
            referrerDomain = referrerArray[2];
            if (referrerDomain !== document.location.host) {
                s.eVar14 = document.referrer;
            }
        }

        s.pageName = document.title;
        s.channel = MEMBERSHIP_STRING;
        s.eVar5 = NONE_STRING;

        user.getMemberDetail(function (memberDetail) {
            if (memberDetail) {
                var tier = memberDetail && (memberDetail.tier && memberDetail.tier.toLowerCase());
                if (tier) {
                    s.eVar5 = tier;
                }
            }
            s.prop2 = GU_ID_STRING + ':' + (identityId ? identityId : NONE_STRING);
            s_code = s.t();

            if (s_code) {
                /*jslint evil: true */
                document.write(s_code);
            }
        });
    }

    return {
        init: init
    };
});
