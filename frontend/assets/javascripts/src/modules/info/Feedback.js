define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/form/Form'
], function ($, bean, component, Form) {
    'use strict';

    var self;
    var FeedbackForm = function () {
        self = this;
    };

    component.define(FeedbackForm);

    FeedbackForm.prototype.classes = {
        FEEDBACK_FORM: 'js-feedback-form'
    };

    FeedbackForm.prototype.init = function () {
        this.setupForm();
    };

    FeedbackForm.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('FEEDBACK_FORM');
        if (formElement) {
            this.form = new Form(formElement);
            this.form.init();
        }
    };

    return FeedbackForm;
});
