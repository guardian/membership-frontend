'use strict';

const PRINT_A_SELECTOR = '.bundle-offering-a__subscribe__offer__print-options';
const PRINT_B_SELECTOR = '.bundle-offering-b__subscribe__offer__print-options';
const PRINT_A_PRICE_SELECTOR = '.bundle-offering-a__print-option__price';
const PRINT_B_PRICE_SELECTOR = '.bundle-offering-b__print-option__price';
const PRINT_A_ID_SELECTOR = '.bundle-offering-a__print-option__id';
const PRINT_B_ID_SELECTOR = '.bundle-offering-b__print-option__id';
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

    for(var i = 0 ; i < PRINT_OPTIONS.length ; i++) {
        var field = PRINT_OPTIONS[i];

        field.addEventListener('click', function (evt) {
            let currentSelection = document.querySelector(CURRENT_PRINT_SELECTOR);
            let target = evt.currentTarget;
            let targetIdElement = target.querySelector(PRINT_A_ID_SELECTOR) || target.querySelector(PRINT_B_ID_SELECTOR);
            let targetId = targetIdElement.textContent.trim();
            let targetPriceElement = target.querySelector(PRINT_A_PRICE_SELECTOR) || target.querySelector(PRINT_B_PRICE_SELECTOR);
            let targetPrice = targetPriceElement.textContent.trim();

            //Update the submit button's price
            PRINT_CTA.querySelector('p').textContent = targetPrice;

            //Update the URL of the button
            PRINT_CTA.href = updateUrlParameter(PRINT_CTA.href, 'selectedOption', targetId);

            //Update status of the fields
            currentSelection.classList.remove('subscribe_option--selected');
            target.classList.add('subscribe_option--selected');
        });
    }
}

function toggle(section) {
    const shouldShowSection =  getComputedStyle(section).display === 'none';
    section.style.display = shouldShowSection ? 'block' : 'none';
}

function updateUrlParameter(url, param, value){
    var regex = new RegExp('('+param+'=)[^\&]+');
    return url.replace( regex , '$1' + value);
}
