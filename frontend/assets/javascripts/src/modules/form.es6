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
 3. payment
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
 - errs (global form errors array)
 - flush (a method to flush the elem and elems on the form - used when validation is added/removed)
 - loader (loader and processing message work)
 - serializer (serialize the form inputs into a data object for sending to stripe)
 */

import validation from 'src/modules/form/validation';
import form from 'src/modules/form/helper/formUtil';
// import payment from 'src/modules/form/payment'; // TODO Can this be deleted?
import address from 'src/modules/form/address';
import options from 'src/modules/form/options';
import submitButton from 'src/modules/form/submitButton';
import ongoingCardPayments from 'src/modules/form/ongoingCardPayments';
import billingPeriodChoice from 'src/modules/form/billingPeriodChoice';
import { init as paypalInit } from 'src/modules/form/paypal';
import { init as stripeInit } from 'src/modules/form/stripe';
import { init as accordionInit } from 'src/modules/form/accordion';
import { init as existEmailInit } from 'src/modules/form/validation/existEmail';
import { loadScript } from 'src/utils/loadScript';

export function init() {

    if (typeof form.elems != 'undefined') {
        validation.init();
        address.init();
        options.init();
        submitButton.init();
        ongoingCardPayments.init();
        billingPeriodChoice.init();

        if (form.hasPaypal) {
            loadScript('//www.paypalobjects.com/api/checkout.js', {}).then(function () {
                paypalInit();
            });
        }

        if (form.hasStripeCheckout) {
            loadScript('//checkout.stripe.com/checkout.js', {})
                .then(function () {
                    stripeInit();
                });
        }

        if (form.hasAccordion){
            accordionInit();
        }

        if (form.hasEmailInput){
            existEmailInit();
        }

        form.attachOphanPageviewId();
    }
}

