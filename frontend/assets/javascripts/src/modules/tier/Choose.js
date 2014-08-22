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
        BENEFIT_HEADER: 'js-benefit-header'
    };

    Choose.prototype.init = function () {
        this.elem = this.getElem('TICKETS_SELECT_FORM');
        this.addListeners();
    };

    Choose.prototype.addListeners = function () {
        var $benefitHeaders = $(this.getClass('BENEFIT_HEADER'));

        console.log($benefitHeaders);

        for(var i = 0, benefitHeadersLength = $benefitHeaders.length; i < benefitHeadersLength; i++) {
            this.addClickEvent($($benefitHeaders[i]));
        }

    };

    Choose.prototype.addClickEvent = function ($benefit) {
        return (function($benefit) {
            bean.on($benefit[0], 'click', function (e) {
                e.preventDefault();
                $benefit.next().toggleClass('hidden-mobile');
            });
        }($benefit));
    };

    return Choose;
});
