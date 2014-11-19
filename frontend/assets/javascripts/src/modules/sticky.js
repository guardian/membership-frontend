define(['$'], function ($) {
    var sticky = $('.js-sticky'),
        stickyTop;

    function scrollHandler() {
        var top = window.pageYOffset || document.documentElement.scrollTop;

        if (!sticky.hasClass('is-sticky')) {
            stickyTop = sticky.offset().top;
        }

        if (top >= stickyTop) {
            sticky.addClass('is-sticky');
        } else {
            sticky.removeClass('is-sticky');
        }
    }

    if (sticky) {
        window.addEventListener('scroll', scrollHandler);
        scrollHandler();
    }
});