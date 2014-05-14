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

        var ctaButtons = document.querySelectorAll('.sign-in');
        var isUserLoggedIn = user.isLoggedIn();

        if(ctaButtons.length && isUserLoggedIn){
            for (var i=0; i<=ctaButtons.length-1; i++) {
                $(ctaButtons[i]).toggleClass(config.classes.LOGGED_IN_CLASS).text(config.text.LOGGED_IN_CTA_BUTTON);
                ctaButtons[i].href = $(ctaButtons[i]).data('url');
            }
        } else if (ctaButtons.length) {
            for (var j=0; j<=ctaButtons.length-1; j++) {
                ctaButtons[j].href = ctaButtons[j].href+'?returnurl='+document.location.href;
            }
        }
    };

    return {
        init: init
    };
});