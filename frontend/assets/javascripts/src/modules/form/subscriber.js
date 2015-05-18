define([
    'bean',
    'ajax',
    'src/utils/text',
    'src/modules/form/validation/display'
], function (bean, ajax, textUtils, display) {

    'use strict';

    var PAYMENT_OPTIONS_CONTAINER_ELEM = document.querySelector('.js-payment-options-container');
    var SUBSCRIBER_PAYMENT_OPTIONS_CONTAINER_ELEM = document.querySelector('.js-subscriber-payment-options-container');
    var SUBSCRIBER_ID_INPUT_ELEM = document.querySelector('.js-subscriber-id-input');
    var SUBSCRIBER_ID_SUBMIT_ELEM = document.querySelector('.js-subscriber-id-submit');
    var POSTCODE_ELEM = document.querySelector('.js-postcode');
    var LAST_NAME_ELEM = document.querySelector('.js-name-last');
    var SUBMIT_INPUT_ELEM = document.querySelector('.js-submit-input');
    var HIDDEN_SUBSCRIBER_INPUT_ELEM = document.querySelector('.js-hidden-subscriber-input');

    function init() {
        if (SUBSCRIBER_ID_INPUT_ELEM && SUBSCRIBER_ID_SUBMIT_ELEM) {
            bean.on(SUBSCRIBER_ID_SUBMIT_ELEM, 'click', function(event) {

                event.preventDefault();

                var subscriberId = textUtils.removeWhitespace(SUBSCRIBER_ID_INPUT_ELEM.value),
                    postcode = textUtils.trimWhitespace(POSTCODE_ELEM.value),
                    lastName = LAST_NAME_ELEM.value;

                if(subscriberId) {
                    ajax({
                        url: '/user/check-subscriber?' + buildQueryString(subscriberId, lastName, postcode)
                    }).then(function (response) {
                        if (response.valid) {
                            handleSuccess(response);
                        } else {
                            handleError(response);
                        }
                    }).fail(handleError);
                } else {
                    display.toggleErrorState({
                        isValid: false,
                        elem: SUBSCRIBER_ID_INPUT_ELEM,
                        msg: 'Please provide your subscriber ID'
                    });
                }
            });
        }
    }

    function buildQueryString(subscriberId, lastName, postcode) {
        var identifierQueryString = 'id=' + subscriberId + '&lastName=' + lastName;
        if(postcode) {
            identifierQueryString += '&postcode=' + postcode;
        }
        return identifierQueryString;
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

        display.toggleErrorState({
            isValid: POSTCODE_ELEM.value !== '',
            elem: POSTCODE_ELEM,
            msg: 'Please provide your Postcode'
        });

        display.toggleErrorState({
            isValid: LAST_NAME_ELEM.value !== '',
            elem: LAST_NAME_ELEM,
            msg: 'Please provide your last name'
        });
    }

    return {
        init: init
    };

});
