/*global zxcvbn */
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

    var checkStrength = function(strengthIndicator, strengthInput) {
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

    var addListeners = function (strengthIndicator, strengthInput) {
        strengthIndicator.classList.toggle(HIDDEN_CLASS);
        strengthInput.addEventListener('keyup', function() {
            checkStrength(strengthIndicator, strengthInput);
        });
    };

    /**
     * Async load in zxcvbn lib as it is ~700kb!
     */
    var init = function() {
        var strengthIndicator = document.querySelector(SELECTORS.strengthIndicator);
        var strengthInput = document.querySelector(SELECTORS.strengthInput);

        if(strengthIndicator && strengthInput) {
            require(['js!zxcvbn'], function() {
                addListeners(strengthIndicator, strengthInput);
            });
        }
    };

    return {
        init: init
    };

});
