/*global zxcvbn */
define([
    '$',
    'bean',
    'src/utils/component'
],
function ($, bean, component) {
    'use strict';

    // TODO The use of these files has changed considerably these files Staff, Free and Paid, Address, Form and Password need a refactor
    // TODO Take out component JS - update to current style of JS and to abstract out common functionality in this class
    // TODO to use on different forms in the init style we use in main and to use data-attributes where applicable.

    var self;

    function Password() {
        self = this;
    }

    component.define(Password);

    Password.prototype.classes = {
        PASSWORD_CONTAINER: 'js-password-container',
        STRENGTH_INDICATOR: 'js-password-strength-indicator',
        STRENGTH_LABEL: 'js-password-strength-label',
        STRENGTH_HAS_INDICATOR: 'has-indicator',
        PASSWORD_STRENGTH_INPUT: 'js-password-strength',
        PASSWORD_STRENGTH_INDICATOR: 'password-strength-indicator',
        TOGGLE_PASSWORD: 'js-toggle-password',
        REGISTER_PASSWORD: 'js-register-password'
    };

    Password.prototype.config = {
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
        ],
        minLength: 6,
        maxLength: 20
    };

    Password.prototype.active = false;

    Password.prototype.$passwordToggleTemplate = function () {

        return $.create([
            '<div class="form-note form-note--right mobile-only">',
                '<a href="#toggle-password" class="text-link ', this.classes.TOGGLE_PASSWORD, '" data-password-label="Show password"',
                    ' data-text-label="Hide password" data-link-name="Toggle password field">Show password</a>',
            '</div>'
        ].join(''));
    };

    Password.prototype.init = function() {

        if (!document.getElementById('user-password')) {
            return;
        }

        this.elem = this.getElem('PASSWORD_CONTAINER');

        require(['js!zxcvbn'], function() {

            $('.' + self.classes.STRENGTH_INDICATOR).toggleClass('is-hidden');

            bean.on(self.getElem('PASSWORD_STRENGTH_INPUT'), 'keyup.count', function () {
                self.checkCount.call(self);
            });

            bean.on(self.getElem('PASSWORD_STRENGTH_INPUT'), 'keyup.key', function () {
                self.checkStrength.call(self);
            });

            self.checkCount();
            self.checkStrength();
        });

        this.toggleInputType();
    };

    Password.prototype.checkCount = function() {
        var passwordInputValue = this.getElem('PASSWORD_STRENGTH_INPUT').value;

        if (passwordInputValue.length >= this.config.minLength) {
            this.active = true;
            $(this.getElem('PASSWORD_STRENGTH_INDICATOR')).removeClass('is-off');
            bean.off(this.getElem('PASSWORD_STRENGTH_INPUT'), 'keyup.count');
        }
    };

    Password.prototype.checkStrength = function() {

        if (this.active) {
            var score = zxcvbn(this.getElem('PASSWORD_STRENGTH_INPUT').value).score;
            var label = this.config.text.passwordLabel + ': ' + this.config.passwordLabels[score];

            if (this.getElem('PASSWORD_STRENGTH_INPUT').value.length < this.config.minLength) {
                label = this.config.text.errorShort;
                score = null;
            } else if (this.getElem('PASSWORD_STRENGTH_INPUT').value.length > this.config.maxLength) {
                label = this.config.text.errorLong;
                score = null;
            }

            this.getElem('PASSWORD_STRENGTH_INDICATOR').className = this.getElem('PASSWORD_STRENGTH_INDICATOR').className.replace(/\bscore-\S+/g, 'score-' + score);
            $(this.getElem('STRENGTH_LABEL')).text(label);
        }
    };

    Password.prototype.toggleInputType = function() {
        var $toggleTemplate = this.$passwordToggleTemplate();
        $toggleTemplate.insertBefore(this.getElem('REGISTER_PASSWORD'));

        bean.on(this.getElem('TOGGLE_PASSWORD'), 'click', function(e) {

            e.preventDefault();
            var target = e.target;
            var inputType = self.getElem('REGISTER_PASSWORD').getAttribute('type') === 'password' ? 'text' : 'password';
            var label = target.getAttribute('data-' + inputType + '-label');

            self.getElem('REGISTER_PASSWORD').setAttribute('type', inputType);
            $(target).text(label);
        });

    };

    return Password;
});
