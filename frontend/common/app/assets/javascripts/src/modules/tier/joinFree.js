define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/Form'
], function ($, bean, component, Form) {
    'use strict';

    var self;
    var JoinFree = function () {
        self = this;
    };

    component.define(JoinFree);

    JoinFree.prototype.classes = {
        NAME_FIRST: 'js-name-first',
        NAME_LAST: 'js-name-last',
        ADDRESS_FORM: 'js-address-form',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code'
    };

    JoinFree.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinFree.prototype.init = function () {
        this.addFormValidation();
    };


    JoinFree.prototype.addFormValidation = function () {
        var formElement = this.elem = this.getElem('ADDRESS_FORM');

        this.form = new Form(formElement);

        this.form.addValidation(
            [
                {
                    elem: this.getElem('NAME_FIRST'),
                    name: 'required'
                },
                {
                    elem: this.getElem('NAME_LAST'),
                    name: 'required'
                },
                {
                    elem: this.getElem('POST_CODE'),
                    name: 'required'
                }
            ]
        ).init();
    };

    return JoinFree;
});
