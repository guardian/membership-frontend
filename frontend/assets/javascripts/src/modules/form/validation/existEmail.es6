import ajax from 'ajax';
import debounce from 'lodash/debounce';

export function init() {
    const emailField = document.getElementById('email');
    const isUseErrorMessage = document.getElementById('form-field__error-message-email-checker');
    const checkerURL = '/user/check-existing-email';

    const validateEmail = debounce(() => {
        let prospectiveEmail = emailField.value;

        if(!prospectiveEmail.includes('@')) {
            return;
        }

        ajax({
            url: checkerURL + '?email=' + encodeURIComponent(emailField.value),
            method: 'get',
            success: ({emailInUse}) => {
                if(emailInUse){
                    isUseErrorMessage.classList.remove('is-hidden');
                }else {
                    isUseErrorMessage.classList.add('is-hidden');
                }
            },
        });

    }, 250);

    emailField.addEventListener('keyup', validateEmail);
}
