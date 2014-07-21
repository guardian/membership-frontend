define([
    '$',
    'bean',
    'src/utils/component',
    'src/utils/Form'
], function ($, bean, component, Form) {
    'use strict';

    var self;
    var Upgrade = function () {
        self = this;
    };

    component.define(Upgrade);

    Upgrade.prototype.classes = {
        STRIPE_FORM: 'js-stripe-form',
        CREDIT_CARD_NUMBER: 'js-credit-card-number',
        CREDIT_CARD_CVC: 'js-credit-card-cvc',
        CREDIT_CARD_EXPIRY_MONTH: 'js-credit-card-exp-month',
        CREDIT_CARD_EXPIRY_YEAR: 'js-credit-card-exp-year',
        BILLING_CTA: 'js-toggle-billing-address',
        BILLING_FIELDSET: 'js-billingAddress-fieldset'
    };

    Upgrade.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    Upgrade.prototype.init = function () {
        this.addFormValidation();
        this.toggleBillingAddressListener();
    };

    Upgrade.prototype.toggleBillingAddressListener = function() {
        bean.on($(this.getElem('BILLING_CTA'))[0], 'click', function () {
            $(self.getElem('BILLING_FIELDSET')).toggleClass('u-h');
        });
    };

    Upgrade.prototype.addFormValidation = function () {
        var formElement = this.elem = this.getElem('STRIPE_FORM'),
            changeToTier = formElement.getAttribute('data-change-to-tier').toLowerCase();

        this.form = new Form(formElement,  '/tier/change/' + changeToTier, '/tier/change/' + changeToTier + '/summary');

        this.form.addValidation(
            [
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

    return Upgrade;
});
