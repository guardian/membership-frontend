define([
    'user'
], function(user){

    var init = function(){

        var ctaButton = document.querySelectorAll('.sign-in')[0];
        var isUserLoggedIn = user.isLoggedIn();

        console.log( isUserLoggedIn );

        if(ctaButton){

        }
    };

    return {
        init: init
    };
});