define([
    '$',
    'user'
], function($, user){

    var classes = {
        loggedInClass: 'sign-in--logged-in'
    };

    var text = {
        loggedInCtaButton: 'Book Event'
    };

    var init = function(){

        var ctaButton = $('.sign-in');
        var isUserLoggedIn = user.isLoggedIn();

        if(ctaButton && isUserLoggedIn){

            ctaButton.toggleClass(classes.loggedInClass).text(text.loggedInCtaButton);
        }
    };

    return {
        init: init
    };
});