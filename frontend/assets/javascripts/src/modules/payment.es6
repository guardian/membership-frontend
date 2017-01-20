// ----- Imports ----- //

import ajax from 'ajax';
import form from 'src/modules/form/helper/formUtil';
import validity from 'src/modules/form/validation/validity';
import serializer from 'src/modules/form/helper/serializer';
import utilsHelper from 'src/utils/helper';
import $ from 'src/utils/$'
import * as paymentError from 'src/modules/form/payment/paymentError';


// ----- Setup ----- //

const $paymentTypes = $('.js-payment-type');
const $spinner = $('.js-payment-processing');


// ----- Exports ----- //

// Clears any payment error messages.
export function clearErrors () {

    paymentError.hideMessage();

}

// When we need to show to payment processing spinner.
export function showSpinner () {

    $paymentTypes.hide();
    clearErrors();

    $spinner.addClass('is-loading');

}

// When we need to hide the payment processing spinner.
export function hideSpinner () {

    $paymentTypes.show();
    $spinner.removeClass('is-loading');

}

// Handles a problem with a payment, takes an error object.
// The error object should have a type and a code, see paymentError.es6.
export function fail (error) {

    hideSpinner();
    paymentError.showMessage(error);

}

// Validates the form; returns true if the form is valid, false otherwise.
export function validateForm () {

    form.elems.map(function (elem) {
        validity.check(elem);
    });

    let formHasErrors = form.errs.length > 0;
    return !formHasErrors;

}

// Creates the new member by posting the form data with the provided token object.
export function postForm (paymentToken) {

    let data = serializer(utilsHelper.toArray(form.elem.elements),
        paymentToken);

    ajax({
        url: form.elem.action,
        method: 'post',
        data: data,
        success: ({redirect}) => window.location.assign(redirect),
        error: fail
    });

}
