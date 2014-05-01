/**
 * Populate user details in the header account information container
 */
define(['user'], function (userUtil) {

    var config = {
        classes: {
            HEADER_ID_CONTAINER: ".identity",
            ID_NOTICE: ".identity__notice",
            ID_ACCOUNT: ".identity__account",
            ID_TIER: ".identity__tier",
            ID_AVATAR: ".identity__avatar"
        }
    }

    return {
        init: function () {
            var user = userUtil.getUserFromCookie();
            if (user) {

                for (var c in config.classes) {
                    config.DOM = config.DOM || {};
                    config.DOM[c] = document.querySelector(config.classes[c]);
                }

                config.DOM.ID_NOTICE.innerHTML = "You are signed in as";

                config.DOM.ID_ACCOUNT.innerHTML = user.displayname;

                config.DOM.ID_TIER.innerHTML = "Guardian Member";

                //config.DOM.ID_AVATAR.innerHTML = "<img src='" + "avatar_url" + "' />";
            }
        }
    }
});