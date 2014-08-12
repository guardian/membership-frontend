define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form'
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
        this.setupForm();
    };


    JoinFree.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('ADDRESS_FORM');
        this.form = new Form(formElement);
        this.form.init();
    };

    return JoinFree;
});
