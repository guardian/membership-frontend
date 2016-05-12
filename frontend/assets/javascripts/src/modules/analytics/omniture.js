/*global s_gi */
define([
    'src/utils/user',
    'src/modules/raven'
], function (user,raven) {
    'use strict';

    var GU_ID_STRING = 'GUID';
    var NONE_STRING = 'none';
    var MEMBERSHIP_STRING = 'Membership';

    function init() {
        curl('js!omniture').then(onSuccess, function(e) {
            raven.Raven.captureException(e, {tags: { level: 'info' }});
        });
    }

    function onSuccess() {
        var s = s_gi('guardiangu-network');
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
            var tier;
            if (memberDetail) {
                tier = memberDetail && (memberDetail.tier && memberDetail.tier.toLowerCase());
                if (tier) {
                    s.eVar5 = tier;
                }
            }
            s.prop2 = GU_ID_STRING + ':' + (identityId ? identityId : NONE_STRING);
            var s_code = s && s.t();

            if (s_code) {
                document.write(s_code);
            }
        });
    }

    return {
        init: init
    };
});
