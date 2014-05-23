/**
 * Populate user details in the header account information container
 */
define(['src/utils/user', '$'], function (userUtil, $) {

    var config = {
        classes: {
            HEADER_ID_CONTAINER: '.identity',
            ID_NOTICE: '.identity__notice',
            ID_ACCOUNT: '.identity__account',
            ID_TIER: '.identity__tier',
            ID_AVATAR: '.identity__avatar'
        }
    };

    return {
        init: function () {
            var user = userUtil.getUserFromCookie();
            if (user) {

                for (var c in config.classes) {
                    config.DOM = config.DOM || {};
                    config.DOM[c] = $(document.querySelector(config.classes[c])); // bonzo object
                }

                config.DOM.ID_NOTICE.text('You are signed in as'); // Screen readers
                config.DOM.ID_NOTICE.addClass('u-h');

                config.DOM.ID_ACCOUNT.text(user.displayname);
                config.DOM.ID_ACCOUNT.removeClass('u-h');

                //config.DOM.ID_TIER.innerHTML = 'Guardian Member';

                config.DOM.ID_AVATAR.addClass('u-h');
                //config.DOM.ID_AVATAR.innerHTML = '<img src="' + 'avatar_url' + '" />';
            }
        }
    };
});