define([
    'src/utils/user',
    'omniture'
], function (user) {

    var GU_ID_STRING = 'GUID';
    var NONE_STRING = 'none';
    var MEMBERSHIP_STRING = 'Membership';

    function init() {
        var pageTitle = document.getElementsByTagName('title')[0].textContent;
        var s = s_gi('guardiangu-network');
        var s_code;
        var identityUser = user.getUserFromCookie();
        var identityId = identityUser && identityUser.id;

        s.pageName = pageTitle;
        s.channel = MEMBERSHIP_STRING;
        s.eVar5 = NONE_STRING;

        user.requestMemberDetail.request(function (memberDetail, err) {
            if (memberDetail) {
                var tier = memberDetail && (memberDetail.tier && memberDetail.tier.toLowerCase());
                if (tier) {
                    s.eVar5 = tier;
                }
            }
            s.prop2 = GU_ID_STRING + ':' + (identityId ? identityId : NONE_STRING);
            s_code = s.t();

            if (s_code) {
                document.write(s_code);
            }
        });
    }

    return {
        init: init
    };
});
