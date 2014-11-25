define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var classes = {
        BENEFIT_HEADER: 'js-benefit-header',
        BENEFIT_BUTTON: 'js-benefit-button'
    };

    var init = function () {
        var elem = $('.' + classes.BENEFIT_HEADER);
        if ( elem.length ) {
            $(elem).each(function (benefitHeader) {
                addClickEvent(benefitHeader);
            }, this);
        }
    };

    var addClickEvent = function (benefitHeader) {
        var $benefitButton = $('.' + classes.BENEFIT_BUTTON, benefitHeader);
        bean.on(benefitHeader, 'click', function (e) {
            e.preventDefault();
            $(benefitHeader).parent().parent().next().toggleClass('hidden-mobile-portrait');
            $benefitButton.toggleClass('toggle--open');
        });
    };

    return {
        init: init
    };

});
