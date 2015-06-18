define([
    '$',
    'src/utils/user'
], function ($, userUtil) {

    var AVAILABLE_DETAILS = ['displayname', 'firstName', 'tier'];
    var CLASS_NAMES = {
        signedOut: 'id--signed-out',
        signedIn: 'id--signed-in',
        hasTier: 'member--has-tier'
    };

    /**
     * Given a user object, checks which details we're interested in
     * and inserts into matching HTML elements.
     */
    function populateUserDetails(obj) {
        AVAILABLE_DETAILS.forEach(function(detail) {
            var detailSelector = '.js-user-' + detail;
            var detailText = obj[detail];
            if (detailText) {
                $(detailSelector).text(detailText).removeClass('u-h');
            }
        });
    }

    /**
     * Updates class on HTML element, adds member tier class as appropriate.
     * Queries cookie and user/me for User details.
     */
    function init() {
        if (userUtil.isLoggedIn()) {
            $(document.documentElement).removeClass(CLASS_NAMES.signedOut).addClass(CLASS_NAMES.signedIn);
            populateUserDetails(userUtil.getUserFromCookie());
            userUtil.getMemberDetail(function (memberDetail) {
                if (memberDetail) {
                    populateUserDetails(memberDetail);

                    if (memberDetail.tier) {
                        $(document.documentElement).addClass(CLASS_NAMES.hasTier);
                    }
                }
            });
        }
    }

    return {
        init: init,
        _: {
            CLASS_NAMES: CLASS_NAMES
        }
    };
});
