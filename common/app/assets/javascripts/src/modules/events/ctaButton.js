define([
    '$',
    'src/utils/user'
], function($, user){

    var config = {
        classes: {
            LOGGED_IN_CLASS: 'action--logged-in',
            LOGGED_OUT_CLASS: 'action--logged-out'
        },
        text: {
            LOGGED_IN_CTA_BUTTON: 'Book Event'
        }
    };

    var init = function(){

        var ctaButtons = document.querySelectorAll('.' + config.classes.LOGGED_OUT_CLASS);
        var isUserLoggedIn = user.isLoggedIn();

        if(ctaButtons.length && isUserLoggedIn){
            for (var i = 0, ctaButtonsLength = ctaButtons.length - 1; i <= ctaButtonsLength; i++) {
                $(ctaButtons[i]).toggleClass(config.classes.LOGGED_IN_CLASS).text(config.text.LOGGED_IN_CTA_BUTTON);
            }
        }
    };

    return {
        init: init
    };
});