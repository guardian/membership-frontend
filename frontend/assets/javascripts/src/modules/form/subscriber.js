define(['bean', 'ajax', 'src/modules/form/validation/display'], function (bean, ajax, display) {
    'use strict';

    var PAYMENT_OPTIONS_CONTAINER_ELEM = document.querySelector('.js-payment-options-container');
    var SUBSCRIBER_PAYMENT_OPTIONS_CONTAINER_ELEM = document.querySelector('.js-subscriber-payment-options-container');
    var SUBSCRIBER_ID_INPUT_ELEM = document.querySelector('.js-subscriber-id-input');
    var SUBSCRIBER_ID_SUBMIT_ELEM = document.querySelector('.js-subscriber-id-submit');
    var POSTCODE_ELEM = document.querySelector('.js-postcode');
    var SUBMIT_INPUT_ELEM = document.querySelector('.js-submit-input');
    var HIDDEN_SUBSCRIBER_INPUT_ELEM = document.querySelector('.js-hidden-subscriber-input');

    function init() {
        if (SUBSCRIBER_ID_INPUT_ELEM && SUBSCRIBER_ID_SUBMIT_ELEM) {
            bean.on(SUBSCRIBER_ID_SUBMIT_ELEM, 'click', function(event) {

                event.preventDefault();

                var subscriberId = SUBSCRIBER_ID_INPUT_ELEM.value,
                    postcode = POSTCODE_ELEM.value;

                ajax({
                    url: '/user/subscriber/details?id='+ subscriberId + '&postcode=' + postcode //todo lastname
                }).then(function(response) {
                    if(response.valid) {
                        handleSuccess(response);
                    } else {
                        handleError(response);
                    }
                }).fail(handleError);
            });
        }
    }

    function handleSuccess(response) {
        /**
         * Disable subscriber number fields
         */
        SUBSCRIBER_ID_SUBMIT_ELEM.setAttribute('disabled', true);
        SUBSCRIBER_ID_INPUT_ELEM.setAttribute('readonly', true);

        /**
         * Toggle payment options
         */
        PAYMENT_OPTIONS_CONTAINER_ELEM.setAttribute('hidden', true);
        SUBSCRIBER_PAYMENT_OPTIONS_CONTAINER_ELEM.removeAttribute('hidden');

        /**
         * Deselect any currently selected options,
         *  select first subscriber option.
         */
        PAYMENT_OPTIONS_CONTAINER_ELEM.querySelector('[checked]').removeAttribute('checked');
        SUBSCRIBER_PAYMENT_OPTIONS_CONTAINER_ELEM.querySelectorAll('[type="radio"]')[0].setAttribute('checked', true);

        /**
         * Update submit label
         */
        SUBMIT_INPUT_ELEM.textContent = 'Join Now';

        /**
         * Update the subscriberOffer hidden input to true
         */
        HIDDEN_SUBSCRIBER_INPUT_ELEM.setAttribute('value', true);

        display.toggleErrorState({
            isValid: response.valid,
            elem: SUBSCRIBER_ID_INPUT_ELEM
        });
    }

    function handleError(response) {
        display.toggleErrorState({
            isValid: response.valid,
            elem: SUBSCRIBER_ID_INPUT_ELEM,
            msg: response.msg
        });
    }

    return {
        init: init
    };

});
