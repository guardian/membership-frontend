define([
    '$',
    'bean',
    'src/utils/component'
], function ($, bean, component) {
    'use strict';

    var self;
    var Choose = function () {
        self = this;
    };

    component.define(Choose);

    Choose.prototype.classes = {
        TICKETS_SELECT_FORM: 'tickets-select-form',
        BENEFIT_HEADER: 'js-benefit-header',
        TOGGLE_ICON: 'js-toggle-icon'
    };

    Choose.prototype.init = function () {
        this.elem = this.getElem('TICKETS_SELECT_FORM');
        if (this.elem) {
            this.addListeners();
        }
    };

    Choose.prototype.addListeners = function () {
        var $benefitHeaders = $(this.getClass('BENEFIT_HEADER'));

        for(var i = 0, benefitHeadersLength = $benefitHeaders.length; i < benefitHeadersLength; i++) {
            this.addClickEvent($($benefitHeaders[i]));
        }
    };

    Choose.prototype.addClickEvent = function ($benefit) {
        bean.on($benefit[0], 'click', function (e) {
            e.preventDefault();
            $benefit.parent().parent().next().toggleClass('hidden-mobile');
            $(self.getElem('TOGGLE_ICON'), $benefit).toggleClass('tickets-select-form__toggle-icon--open');
        });
    };

    return Choose;
});
