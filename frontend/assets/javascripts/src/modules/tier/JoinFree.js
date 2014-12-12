define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form',
    'src/utils/form/Password',
    'src/utils/form/Address'
], function ($, bean, component, Form, Password, Address) {
    'use strict';

    // TODO The use of these files has changed considerably these files Staff, Free and Paid, Address, Form and Password need a refactor
    // TODO Take out component JS - update to current style of JS and to abstract out common functionality in this class
    // TODO to use on different forms in the init style we use in main and to use data-attributes where applicable.

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
