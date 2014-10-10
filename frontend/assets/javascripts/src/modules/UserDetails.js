define([
    '$',
    'src/utils/user'
], function ($, userUtil) {

    function UserDetails() {}

    UserDetails.prototype.availableDetails = ['displayname', 'primaryemailaddress', 'firstName', 'tier'];

    /**
     * Given a user object, checks which details we're interested in
     * and inserts into matching HTML elements.
     */
    UserDetails.prototype.populateUserDetails = function (obj) {
        for (var i = 0; i < this.availableDetails.length; i++) {
            var detail = this.availableDetails[i];
            if (detail) {
                $('.js-user-' + detail).removeClass('u-h').text(obj[detail]);
            }
        }
    };

    /**
     * Updates ID class on HTML element if required, adds member tier class as appropriate.
     * Queries cookie and user/me for User details.
     */
    UserDetails.prototype.init = function () {
        var self = this;
        if (userUtil.isLoggedIn()) {
            $(document.documentElement).removeClass('id--signed-out').addClass('id--signed-in');

            var identityDetail = userUtil.getUserFromCookie();
            self.populateUserDetails(identityDetail);

            userUtil.getMemberDetail(function (memberDetail) {
                if (memberDetail) {
                    self.populateUserDetails(memberDetail);

                    if (memberDetail.tier) {
                        $(document.documentElement).addClass('member--has-tier');
                    }
                }
            });
        }
    };

    return UserDetails;
});
