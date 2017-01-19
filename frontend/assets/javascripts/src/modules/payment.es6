// ----- Imports ----- //

import ajax from 'ajax';
import form from 'src/modules/form/helper/formUtil';
import validity from 'src/modules/form/validation/validity';
import serializer from 'src/modules/form/helper/serializer';
import utilsHelper from 'src/utils/helper';
import $ from 'src/utils/$'
import * as error from 'src/modules/form/payment/paymentError';


// ----- Setup ----- //

const $paymentTypes = $('.js-payment-type');
const $spinner = $('.js-payment-processing');


// ----- Functions ----- //

export function open() {
    //When a payment method "overlay" is opened.
    $paymentTypes.hide();
    $spinner.addClass('is-loading');
}

export function close() {
    //When a payment method overlay is closed.
    $paymentTypes.show();
    $spinner.removeClass('is-loading');
}

// Handles a problem with a payment, takes an error object.
// The error object should have a type and a code, see paymentError.es6.
export function fail (error) {

    error.showMessage(error);

}

// Validates the form; returns true if the form is valid, false otherwise.
export function validateForm() {

    form.elems.map(function (elem) {
        validity.check(elem);
    });

    let formHasErrors = form.errs.length > 0;
    return !formHasErrors;

}

// Creates the new member by posting the form data with the provided token object.
export function postForm(paymentToken) {

    let data = serializer(utilsHelper.toArray(form.elem.elements),
        paymentToken);

    ajax({
        url: form.elem.action,
        method: 'post',
        data: data,
        success: function (successData) {
            window.location.assign(successData.redirect);
        },
        error: function (errData) {
            alert(err);
        }
    });

}
