(function() {
    function id(id) { return document.getElementById(id) };
    id('address-line-one-deliveryAddress').value = 'Address Line 1';
    id('address-line-two-deliveryAddress').value = 'Address Line 2';
    id('town-deliveryAddress').value = 'Town';
    id('county-or-state-deliveryAddress').value = 'County';
    id('postCode-deliveryAddress').value = 'Postcode';
    id('cc-num').value = '4242424242424242';
    id('cc-cvc').value = '123';
    id('cc-exp-month').value = '3';
    id('cc-exp-year').value = '2025';
    document.querySelector('.js-submit-input').click();
})();
