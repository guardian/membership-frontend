define(['$', 'src/utils/helper'], function ($, utilsHelper) {

    var sticky = $('.js-sticky'),
        stickyLink = $( sticky.attr('data-sticky-sibling') ),
        stickyTop;

    var breakpoint = window.getComputedStyle(document.body, ':after').getPropertyValue('content');

    function checkSiblingHeight() {
        var run = true,
            heightEl, heightSiblingEl;

        if (stickyLink) {
            heightEl = utilsHelper.getOuterHeight(sticky.get(0));
            heightSiblingEl = utilsHelper.getOuterHeight(stickyLink.get(0));
            run = (heightSiblingEl >  heightEl) ? true : false;
        }
        return run;
    }

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

    function init() {
        if ( breakpoint !== 'mobile' && sticky.length ) {
            if ( checkSiblingHeight() ) {
                window.addEventListener('scroll', scrollHandler);
                scrollHandler();
            }
        }
    }

    return {
        init: init
    };


});
