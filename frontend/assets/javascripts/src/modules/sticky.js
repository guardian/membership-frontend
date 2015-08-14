define(['$', 'src/utils/helper'], function ($, helper) {
    'use strict';

    var sticky = $('.js-sticky'),
        stickyLink = $(sticky.attr('data-sticky-sibling')),
        stickyTop;

    function checkSiblingHeight() {
        var run = true,
            heightEl, heightSiblingEl;

        if (stickyLink.length) {
            heightEl = helper.getOuterHeight(sticky.get(0));
            heightSiblingEl = helper.getOuterHeight(stickyLink.get(0));
            run = (heightSiblingEl > heightEl) ? true : false;
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
        if (helper.getBreakpoint() !== 'mobile' && sticky.length) {
            if (checkSiblingHeight()) {
                window.addEventListener('scroll', scrollHandler);
                scrollHandler();
            }
        }
    }

    return {
        init: init
    };


});
