import * as payment from 'src/modules/payment';
import bean from 'bean'
import $ from '$'
export function init() {
    let handler = window.StripeCheckout.configure(guardian.stripeCheckout);
    let success = false;
    const button = $('.js-stripe-checkout');
    bean.on(window, 'popstate', handler.close);
    const amount = () => {

        let billingPeriod = guardian.membership.checkoutForm.billingPeriods[guardian.membership.checkoutForm.billingPeriod];
        let amount = billingPeriod.generateDisplayAmount();
        let period = billingPeriod.noun;
        const monthlyContributionField = $('.js-monthly-contribution');
        if (monthlyContributionField.length > 0){
            amount = "£" + parseFloat(monthlyContributionField[1].value);
        }
        return "Pay " + amount + " per " + period;
    };
    const open = (e) => {
        if (payment.validateForm()) {
            const email = document.querySelector('#email').value;
            payment.showSpinner();
            handler.open({
                description: 'Please enter your card details.',
                panelLabel: amount(),
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
