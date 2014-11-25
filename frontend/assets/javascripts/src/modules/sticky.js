define(['$'], function ($) {
    var sticky = $('.js-sticky'),
        stickyTop;

    var breakpoint = window.getComputedStyle(document.body, ':after').getPropertyValue('content');

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

    if (breakpoint !== 'mobile' && sticky.length) {
        window.addEventListener('scroll', scrollHandler);
        scrollHandler();
    }
});
