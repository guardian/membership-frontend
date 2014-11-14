define([
    '$',
    'bean',
    'src/utils/component'
], function ($, bean, component) {
    'use strict';

    var Choose = function () {};

    component.define(Choose);

    Choose.prototype.classes = {
        TICKETS_SELECT_FORM: 'js-select-tier',
        BENEFIT_HEADER: 'js-benefit-header',
        BENEFIT_BUTTON: 'js-benefit-button',
        TOGGLE_ICON: 'js-toggle-icon'
    };

    Choose.prototype.init = function () {
        this.elem = this.getElem('TICKETS_SELECT_FORM');
        if (this.elem) {
            $(this.getClass('BENEFIT_HEADER')).each(function (benefitHeader) {
                this.addClickEvent(benefitHeader);
            }, this);
        }
    };

    Choose.prototype.addClickEvent = function (benefitHeader) {
        var $benefitButton = $(this.getClass('BENEFIT_BUTTON'), benefitHeader);
        bean.on(benefitHeader, 'click', function (e) {
            e.preventDefault();
            $(benefitHeader).parent().parent().next().toggleClass('hidden-mobile-portrait');
            $benefitButton.toggleClass('toggle--open');
        });
    };

    return Choose;
});
