import * as payment from 'src/modules/payment';
import bean from 'bean'
import $ from '$'
export function init() {
    let handler = window.StripeCheckout.configure(guardian.stripeCheckout);
    let success = false;
    const button = $('.js-stripe-checkout');
    const email = button.data('email');
    bean.on(window, 'popstate', handler.close);

    const open = (e) => {
        if (payment.validateForm()) {
            payment.showSpinner();
            handler.open({
                description: 'So I could do with some good copy for this, and what it needs to contain like price etc.',
                panelLabel: 'Approved button copy!',
                email: email,
                token: (token) => {
                    success = true;
                    payment.postForm({
                        'payment.stripeToken': token.id
                    })
                },
                closed: () => {
                    if (!success) {
                        payment.hideSpinner();
                    }
                }
            })
        }
        e.preventDefault();
    };

    bean.on(button[0], 'click', open);


}
