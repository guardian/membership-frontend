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

    // TODO abstract out common functionality in this class
    var JoinPaid = function() {
        self = this;
    };

    component.define(JoinPaid);

    JoinPaid.prototype.classes = {
        STRIPE_FORM: 'js-stripe-form',
        POST_CODE: 'js-post-code',
        CTA_PAYMENT_OPTION_PRICE: 'js-cta-payment-option-price',
        PAYMENT_OPTIONS_CONTAINER: 'js-payment-options-container',
        CVC_CTA: 'js-cvc-cta',
        CVC_DESCRIPTION_CONTAINER: 'js-cvc-image-container',
        CARD_DETAILS_NOTE: 'js-card-details-note',
        CARD_NOTE_CHARGE: 'js-card-note-pricing-charge',
        CARD_NOTE_PERIOD: 'js-card-note-pricing-period',
        CARD_NOTE_PAYMENT_TAKEN: 'js-card-note-payment-taken'
    };

    JoinPaid.prototype.data = {
        CARD_TYPE: 'data-card-type'
    };

    JoinPaid.prototype.init = function () {
        this.setupForm();
        this.setupCtaPaymentOptionPriceListeners();
        this.setupCvcToggle();
    };


    // setup form, passwordStrength, Address toggle for delivery and billing
    JoinPaid.prototype.setupForm = function () {
        var formElement = this.elem = this.getElem('STRIPE_FORM');
        var addressHelper;

        this.form = new Form(formElement, '/subscription/subscribe', window.location.href.replace('enter-details', 'thankyou'));
        this.form.init();

        addressHelper = new Address(this.form);
        addressHelper.setupDeliveryToggleState();
        addressHelper.setupToggleBillingAddressListener();

        (new Password()).init();
    };

    JoinPaid.prototype.setupCtaPaymentOptionPriceListeners = function() {
        var $paymentOptionsContainer = $(this.getClass('PAYMENT_OPTIONS_CONTAINER'), this.form.formElement);
        var $ctaOptionPrice = $(this.getClass('CTA_PAYMENT_OPTION_PRICE'), this.form.formElement);

        bean.on($paymentOptionsContainer[0], 'click', 'input', function (e) {
            var input = e && e.target;
            var paymentOptionPrice = input.getAttribute('data-pricing-option-amount');
            $ctaOptionPrice.text(paymentOptionPrice);
            self.populateCardNote(input.value);
        });
    };

    JoinPaid.prototype.populateCardNote = function(period) {
        var $cardNote = $(this.getElem('CARD_DETAILS_NOTE'));
        var $cardNoteCharge = $(this.getClass('CARD_NOTE_CHARGE'), $cardNote);
        var $cardNotePeriod = $(this.getClass('CARD_NOTE_PERIOD'), $cardNote);
        var $cardNotePaymentTaken = $(this.getClass('CARD_NOTE_PAYMENT_TAKEN'), $cardNote);

        $cardNoteCharge.text($cardNoteCharge.attr('data-' + period));
        $cardNotePeriod.text($cardNotePeriod.attr('data-' + period));
        $cardNotePaymentTaken.html($cardNotePaymentTaken.attr('data-' + period));
    };


    JoinPaid.prototype.setupCvcToggle = function() {
        var $cvcDescriptionContainer = $(this.getClass('CVC_DESCRIPTION_CONTAINER'), this.form.formElement);
        var $cvcCta = $(this.getClass('CVC_CTA'), this.form.formElement);

        bean.on($cvcCta[0], 'click', function (e) {
            e.preventDefault();
            $cvcDescriptionContainer.toggleClass('u-h');
        });
    };

    return JoinPaid;
});
