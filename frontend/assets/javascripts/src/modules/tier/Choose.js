define([
    '$',
    'bean',
    'src/utils/component'
], function ($, bean, component) {
    'use strict';

    var Choose = function () {};

    component.define(Choose);

    Choose.prototype.classes = {
        BENEFIT_CONTAINER: 'js-benefit-container',
        BENEFIT_HEADER: 'js-benefit-header',
        BENEFIT_BUTTON: 'js-benefit-button',
        TOGGLE_ICON: 'js-toggle-icon'
    };

    Choose.prototype.init = function () {
        this.elem = this.getElem('BENEFIT_CONTAINER');

        if (this.elem) {
            $(this.getElem('BENEFIT_HEADER')).each(function (benefitHeader) {
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
