/**
 * This file:
 * Controls the identity icon found in the header cta text "Sign in" or "You are signed in as"
 * Appends the user id to the comment activity link when there is a user
 * Updates the edit profile link when there is a user with a membership tier to the membership edit profile link
 * Controls the "Join Us" cta visibility
 */
define(['src/utils/user'], function (userUtil) {

    var IS_HIDDEN = 'is-hidden';
    var SIGNED_IN_TEXT = 'You are signed in as';
    var MENU_TEXT_ELEM = document.querySelector('.js-identity-menu-text');
    var MENU_EDIT_PROFILE_ELEM = document.querySelector('.js-identity-menu-edit-profile');
    var MENU_COMMENT_ACTIVITY_ELEM = document.querySelector('.js-identity-menu-comment-activity');
    var HEADER_JOIN_US_CTA_ELEM = document.querySelector('.js-header-join-us-cta');

    function init() {
        var identityUser = userUtil.getUserFromCookie();

        if (identityUser) {
            hideIdentityCtaText();
            appendUserIdToCommentActivityLink(identityUser.id);

            userUtil.getMemberDetail(function (memberDetail) {
                if (memberDetail && memberDetail.tier) {
                    updateEditProfileLink();
                }
            });
        } else {
            showJoinUsCta();
        }
    }

    function hideIdentityCtaText() {
        if(MENU_TEXT_ELEM) {
            MENU_TEXT_ELEM.textContent = SIGNED_IN_TEXT;
            MENU_TEXT_ELEM.classList.add('u-h');
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

    function showJoinUsCta() {
        HEADER_JOIN_US_CTA_ELEM.classList.remove(IS_HIDDEN);
    }

    return {
        init: init
    };
});
