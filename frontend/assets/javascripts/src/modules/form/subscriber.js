define(['bean', 'ajax', 'src/modules/form/validation/display'], function (bean, ajax, display) {
    'use strict';

    var SUBSCRIBER_ID_INPUT = document.querySelector('.js-subscriber-id-input');
    var SUBSCRIBER_ID_SUBMIT = document.querySelector('.js-subscriber-id-submit');
    var POSTCODE_ELEM = document.querySelector('.js-postcode');

    var init = function() {
        if (SUBSCRIBER_ID_INPUT && SUBSCRIBER_ID_SUBMIT) {
            bean.on(SUBSCRIBER_ID_SUBMIT, 'click', function(e) {
                e.preventDefault();

                var subscriberId = SUBSCRIBER_ID_INPUT.value;
                var postcode = POSTCODE_ELEM.value;

                ajax({
                    url: '/user/subscriber/details?id='+ subscriberId + '&postcode=' + postcode //todo get postcode & lastname
                }).then(function(respsonse) {
                    if(respsonse.valid) {
                        //todo remove and process the response. Just trying to get past jshint and commit this
                        display.toggleErrorState({
                            isValid: false,
                            elem: SUBSCRIBER_ID_INPUT
                        });
                    } else {
                        display.toggleErrorState({
                            isValid: false,
                            elem: SUBSCRIBER_ID_INPUT
                        });
                    }
                }).fail(function() {
                    display.toggleErrorState({
                        isValid: false,
                        elem: SUBSCRIBER_ID_INPUT
                    });
                });
            });
        }
    };

    return {
        init: init
    };

});
