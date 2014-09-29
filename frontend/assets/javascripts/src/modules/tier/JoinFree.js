define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form',
    'src/utils/form/Password',
    'src/utils/form/Address'
], function ($, bean, component, Form, Password, Address) {
    'use strict';

    var self;

    var JoinFree = function () {
        self = this;
    };

    component.define(JoinFree);

    JoinFree.prototype.classes = {
        ADDRESS_FORM: 'js-address-form'
    };

    JoinFree.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinFree.prototype.init = function () {
        var formElement = this.elem = this.getElem('ADDRESS_FORM');
        if (formElement) {
            var addressHelper;

            this.form = new Form(formElement);
            this.form.init();

            addressHelper = new Address(this.form);
            addressHelper.setupDeliveryToggleState();

            (new Password()).init();
        }
    };

    return JoinFree;
});
