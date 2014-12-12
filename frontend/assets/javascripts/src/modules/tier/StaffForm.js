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

    var StaffForm = function () {};

    component.define(StaffForm);

    StaffForm.prototype.classes = {
        STAFF_FORM: 'js-staff-form'
    };

    StaffForm.prototype.init = function () {
        this.elem = this.getElem('STAFF_FORM');
        if (this.elem) {
            this.setupForm();
        }
    };

    // setup form, passwordStrength, Address toggle for delivery and billing
    StaffForm.prototype.setupForm = function () {

        this.form = new Form(this.elem);
        this.form.init();

        var addressHelper = new Address(this.form);
        addressHelper.setupDeliveryToggleState();

        (new Password()).init();
    };

    return StaffForm;
});
