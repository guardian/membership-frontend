/*global zxcvbn */
define(['bean'], function (bean) {
    'use strict';

    var STRENGTH_INDICATOR_ELEM = document.querySelector('.js-password-strength-indicator');
    var USER_PASSWORD_ELEM = document.getElementById('user-password');
    var PASSWORD_STRENGTH_INPUT_ELEM = document.querySelector('.js-password-strength');
    var STRENGTH_LABEL_ELEM = document.querySelector('.js-password-strength-label');
    var config = {
        text: {
            passwordLabel: 'Password strength',
            errorLong: 'Password too long',
            errorShort: 'Password too short'
        },
        passwordLabels: [
            'weak',
            'poor',
            'medium',
            'good',
            'strong'
        ]
    };

    var init = function() {
        if (USER_PASSWORD_ELEM) {
            addListeners();
        }
    };

    /**
     * load in zxcvbn lib as it is ~700kb!
     * setup listener for length check
     * setup listener for zxcvbn.score check
     */
    var addListeners = function () {
        require(['js!zxcvbn'], function() {
            STRENGTH_INDICATOR_ELEM.classList.toggle('is-hidden');

            bean.on(PASSWORD_STRENGTH_INPUT_ELEM, 'keyup', function () {
                checkStrength();
            });
        });
    };

    /**
     * check zxcvbn score and apply relevant className to display strength
     */
    var checkStrength = function() {
        var score = zxcvbn(PASSWORD_STRENGTH_INPUT_ELEM.value).score;
        var label = config.text.passwordLabel + ': ' + config.passwordLabels[score];

        if (PASSWORD_STRENGTH_INPUT_ELEM.value.length < PASSWORD_STRENGTH_INPUT_ELEM.getAttribute('minlength')) {
            label = config.text.errorShort;
            score = null;
        } else if (PASSWORD_STRENGTH_INPUT_ELEM.value.length > PASSWORD_STRENGTH_INPUT_ELEM.getAttribute('maxlength')) {
            label = config.text.errorLong;
            score = null;
        }

        STRENGTH_INDICATOR_ELEM.className = STRENGTH_INDICATOR_ELEM.className.replace(/\bscore-\S+/g, 'score-' + score);
        STRENGTH_LABEL_ELEM.textContent = label;
    };

    return {
        init: init
    };
});
