/**
 * This file:
 * Controls the identity icon found in the header cta text "Sign in" or "You are signed in as"
 * Appends the user id to the comment activity link when there is a user
 * Updates the edit profile link when there is a user with a membership tier to the membership edit profile link
 */
define(['src/utils/user'], function (userUtil) {
    'use strict';

    var MENU_EDIT_PROFILE_ELEM = document.querySelector('.js-identity-menu-edit-profile');
    var MENU_COMMENT_ACTIVITY_ELEM = document.querySelector('.js-identity-menu-comment-activity');

    function init() {
        var identityUser = userUtil.getUserFromCookie();
        if (identityUser) {
            appendUserIdToCommentActivityLink(identityUser.id);
            userUtil.getMemberDetail(function (memberDetail) {
                if (memberDetail && memberDetail.tier) {
                    updateEditProfileLink();
                }
            });
        }
    }

    function updateEditProfileLink() {
        MENU_EDIT_PROFILE_ELEM.setAttribute('href',
            MENU_EDIT_PROFILE_ELEM.getAttribute('data-member-href')
        );
    }

    function appendUserIdToCommentActivityLink(id) {
        MENU_COMMENT_ACTIVITY_ELEM.setAttribute('href',
                MENU_COMMENT_ACTIVITY_ELEM.getAttribute('href') + id
        );
    }

    return {
        init: init
    };
});
