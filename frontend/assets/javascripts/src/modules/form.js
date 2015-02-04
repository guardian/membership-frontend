/*
 Form JS

 Validation
 Add the 'noValidate' attribute to the form to stop html5 validation and add the class .js-form to use the form interaction work.
 The validation now works with attributes on inputs. We use the required, minlength, maxlength, data-validation, pattern attributes to validate.
 The data-validation attribute is used when we need custom validation such as client side checking of credit card type, or month and year validation.

 form.js
 form.js which is a facade to all form interactivity across the site. form.js initialises the:
 1. validation
    - adds form listeners (blur/change and submit)
 2. address rules
    - adds listeners for the address rules (subdivision choice and address validation) for delivery and billing addresses
    - sets up the billing address CTA toggle
 3. password
    - sets up the password check work
 4. payment
    - sets up stripe
    - adds payment listeners for masker and displaying the card image
    - sets up payment options listeners to provide different info on payment option choice

 All of these initialisations follow our pattern in main.js and will only apply the relevant work if the relevant elements are on the page.

 form.js module grouping
 The form work is split into four major groups to make it easier to manage and test the code:

 Validation
 - listeners (submit, input, select listeners)
 - actions (add/remove validation)
 - display (show local and global form errors, toggle and reset error state)
 - validity (validation)

 Payment
 - listeners (listeners for masker and display card image in the input)
 - options (setup toggle work to change text around the form on payment option toggle)
 - processing (stripe work for handling the jsonp token call and ajax call to our backend for payment)
 - validationProfiles (profiles for various payment validation)

 Address
 - billing (setup billing address CTA and toggle work)
 - rules (set up form for subdivision and postcode rules/validation dependant on country selected)
 - postcode (postcode validation work)
 - subdivision (state/county/province work)

 Helper
 - formUtil (a singleton to provide an object for useful form properties/methods)
     - elem (form dom element)
     - elems (form elements to validate)
     - hasPayment (method to tell us if the form has payment)
     - errs (global form errors array)
     - flush (a method to flush the elem and elems on the form - used when validation is added/removed)
 - loader (loader and processing message work)
 - password (password work)
 - serializer (serialize the form inputs into a data object for sending to stripe)
 */

define([
    'src/modules/form/validation',
    'src/modules/form/helper/formUtil',
    'src/modules/form/payment',
    'src/modules/form/address',
    'src/modules/form/helper/password'
], function (validation, form, payment, address, password) {
    'use strict';

    var init = function () {
        if (form) {
            validation.init();
            address.init();
            password.init();

            if (form.hasPayment) {
                payment.init();
            }
        }
    };

    return {
        init: init
    };
});
