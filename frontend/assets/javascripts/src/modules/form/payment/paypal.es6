import form from 'src/modules/form/helper/formUtil';
import validity from 'src/modules/form/validation/validity';

export function init () {

	paypal.Button.render({
				
		env: 'sandbox', // Specify 'production' for the prod environment
		style: {
			color: 'blue',
			size: 'medium'
		},

		// Called when user clicks Paypal button.
		payment: function (resolve, reject) {

			form.elems.map(function (elem) {
				validity.check(elem);
			});

			if (form.errs.length > 0) {
				reject();
			} else {

				const SETUP_PAYMENT_URL = '/paypal-setup-payment';

				paypal.request.post(SETUP_PAYMENT_URL)
					.then(data => {
						resolve(data.token);
					})
					.catch(err => {
						reject(err);
					});

			}

		},

		// Called when user finishes with Paypal interface (approves payment).
		onAuthorize: function (data, actions) {

			const CREATE_AGREEMENT_URL = '/paypal-create-agreement';

			fetch(CREATE_AGREEMENT_URL, {
				headers: { 'Content-Type': 'application/json' },
				method: 'POST',
				body: JSON.stringify({ token: data.paymentToken })
			}).then(response => {
				console.log(response);
			}).catch(err => {
				alert(err);
			});

	   }
			
	}, '#paypal-button-checkout');

}
