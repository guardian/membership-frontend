define([
    '$',
    'src/utils/user'
], function($, user){

    var config = {
        classes: {
            LOGGED_IN_CLASS: 'sign-in--logged-in'
        },
        text: {
            LOGGED_IN_CTA_BUTTON: 'Book Event'
        }
    };

    var init = function(){

        var $ctaButton = $('.sign-in');
        var isUserLoggedIn = user.isLoggedIn();

        if($ctaButton && isUserLoggedIn){

            $ctaButton.toggleClass(config.classes.LOGGED_IN_CLASS).text(config.text.LOGGED_IN_CTA_BUTTON);
            $ctaButton[0].href = $ctaButton.data('url');
        }
    };

    return {
        init: init
    };
});