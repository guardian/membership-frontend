define(function () {
    'use strict';

    var SELECTORS = {
        strengthIndicator: '.js-password-strength-indicator',
        strengthInput: '.js-password-strength',
        strengthLabel: '.js-password-strength-label'
    };
    var HIDDEN_CLASS = 'is-hidden';
    var CONFIG = {
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

    var checkStrength = function(zxcvbn, strengthIndicator, strengthInput) {
        var score = zxcvbn(strengthInput.value).score;
        var label = CONFIG.text.passwordLabel + ': ' + CONFIG.passwordLabels[score];
        var strengthLabel = document.querySelector(SELECTORS.strengthLabel);

        if (strengthInput.value.length < strengthInput.getAttribute('minlength')) {
            label = CONFIG.text.errorShort;
            score = null;
        } else if (strengthInput.value.length > strengthInput.getAttribute('maxlength')) {
            label = CONFIG.text.errorLong;
            score = null;
        }

        strengthIndicator.className = strengthIndicator.className.replace(/\bscore-\S+/g, 'score-' + score);
        if(strengthLabel) {
            strengthLabel.textContent = label;
        }
    };

    var addListeners = function (zxcvbn, strengthIndicator, strengthInput) {
        strengthIndicator.classList.toggle(HIDDEN_CLASS);
        strengthInput.addEventListener('keyup', function() {
            checkStrength(zxcvbn, strengthIndicator, strengthInput);
        });
    };

    var init = function() {
        var strengthIndicator = document.querySelector(SELECTORS.strengthIndicator);
        var strengthInput = document.querySelector(SELECTORS.strengthInput);

        if(strengthIndicator && strengthInput) {
            /**
             * Async load in zxcvbn lib as it is ~700kb!
             * Loads as an AMD module as of version ~3.5
             */
            require(['zxcvbn'], function(zxcvbn) {
                addListeners(zxcvbn, strengthIndicator, strengthInput);
            });
        }
    };

    return {
        init: init
    };

});
