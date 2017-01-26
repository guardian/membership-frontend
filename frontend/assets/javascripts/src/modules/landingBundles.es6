'use strict';

const PRINT_A_SELECTOR = '.bundle-offering-a__subscribe__offer__print-options';
const PRINT_B_SELECTOR = '.bundle-offering-b__subscribe__offer__print-options';
const PRINT_A_PRICE_SELECTOR = '.bundle-offering-a__print-option__price';
const PRINT_B_PRICE_SELECTOR = '.bundle-offering-b__print-option__price';
const PRINT_A_NAME_SELECTOR = '.bundle-offering-a__print-option__description >h1';
const PRINT_B_NAME_SELECTOR = '.bundle-offering-a__print-option__description >h1';
const SEE_MORE_CTA_SELECTOR = '.js-see-more-button';
const CURRENT_PRINT_SELECTOR = '.subscribe_option--selected';
const PRINT_CTA_SELECTOR = '.js-print-button';
const PRINT_OPTIONS_SELECTOR = '.bundle-offering-a__subscribe__offer__print-options__option, .bundle-offering-b__subscribe__offer__print-options__option';

const PRINT_A = document.querySelectorAll(PRINT_A_SELECTOR);
const PRINT_B = document.querySelectorAll(PRINT_B_SELECTOR);
const SEE_MORE_CTA = document.querySelector(SEE_MORE_CTA_SELECTOR);
const PRINT_OPTIONS = document.querySelectorAll(PRINT_OPTIONS_SELECTOR);
const PRINT_CTA = document.querySelector(PRINT_CTA_SELECTOR);


export function init() {

    if(PRINT_A.length == 0 && PRINT_B.length == 0){
        return;
    }

    bindPrintEvents();
}

function bindPrintEvents(){
    bindButtonBehaviour();
    bindOptionsBehaviour();
}

function bindButtonBehaviour() {
    SEE_MORE_CTA.addEventListener('click', function(evt){
        let printSection = PRINT_A[0] || PRINT_B[0];
        toggle(printSection);
        toggle(SEE_MORE_CTA);
        evt.preventDefault();
    });
}

function bindOptionsBehaviour() {
    PRINT_OPTIONS.forEach(function(field) {
        field.addEventListener('click', function(evt){
            let currentSelection = document.querySelector(CURRENT_PRINT_SELECTOR);
            let target = evt.currentTarget;
            let nameElement = target.querySelector(PRINT_A_NAME_SELECTOR) || target.querySelector(PRINT_B_NAME_SELECTOR);
            let nameText = nameElement.textContent.trim();
            let priceElement = target.querySelector(PRINT_A_PRICE_SELECTOR) || target.querySelector(PRINT_B_PRICE_SELECTOR);
            let priceText = priceElement.textContent.trim();
            PRINT_CTA.querySelector('p').textContent = priceText;
            PRINT_CTA.href = PRINT_CTA.href.split('-')[0] + '-' + nameText;
            currentSelection.classList.remove('subscribe_option--selected');
            target.classList.add('subscribe_option--selected');
        });

    });
}

function toggle(section) {
    if(getComputedStyle(section).display === 'none') {
        section.style.display = 'block';
    } else {
        section.style.display = 'none';
    }
}
