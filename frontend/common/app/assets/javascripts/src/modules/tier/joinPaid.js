define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/Form'
], function ($, bean, component, Form) {
    'use strict';

    var self;
    var JoinPaid = function () {
        self = this;
    };

    component.define(JoinPaid);

    JoinPaid.prototype.classes = {
        NAME_FIRST: 'js-name-first',
        NAME_LAST: 'js-name-last',
        STRIPE_FORM: 'js-stripe-form',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        ADDRESS_LINE_ONE: 'js-address-line-one',
        TOWN: 'js-town',
        POST_CODE: 'js-post-code',
        BILLING_CTA: 'js-toggle-billing-address',
        BILLING_FIELDSET: 'js-billingAddress-fieldset'
    };

    JoinPaid.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinPaid.prototype.init = function () {
        this.addFormValidation();
        this.toggleBillingAddressListener();
    };

    JoinPaid.prototype.toggleBillingAddressListener = function() {
        bean.on($(this.getElem('BILLING_CTA'))[0], 'click', function () {
            $(self.getElem('BILLING_FIELDSET')).toggleClass('u-h');
        });
    };

    JoinPaid.prototype.addFormValidation = function () {
        var formElement = this.elem = this.getElem('STRIPE_FORM');

        this.form = new Form(formElement, '/subscription/subscribe', window.location.href.replace('payment', 'thankyou'));

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
                    elem: this.getElem('ADDRESS_LINE_ONE'),
                    name: 'required'
                },
                {
                    elem: this.getElem('TOWN'),
                    name: 'required'
                },
                {
                    elem: this.getElem('POST_CODE'),
                    name: 'required'
                },
                {
                    elem: this.getElem('CREDIT_CARD_NUMBER'),
                    name: 'creditCardNumber'
                },
                {
                    elem: this.getElem('CREDIT_CARD_CVC'),
                    name: 'creditCardCVC'
                },
                {
                    elem: [
                        this.getElem('CREDIT_CARD_EXPIRY_MONTH'),
                        this.getElem('CREDIT_CARD_EXPIRY_YEAR')
                    ],
                    name: 'creditCardExpiry'
                }
            ]
        ).init();
    };

    return JoinPaid;
});
