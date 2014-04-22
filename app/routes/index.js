var express = require('express');
var router = express.Router();

var stripe = require('stripe')("***REMOVED***");

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Membership prototypes' });
});

/* GET home page. */
router.get('/stripe', function(req, res) {
  res.render('stripe', { title: 'Stripe' });
});

router.post('/stripe', function(req, res) {
    // Set your secret key: remember to change this to your live secret key in production
    // See your keys here https://manage.stripe.com/account
    stripe.setApiKey("***REMOVED***");

    // (Assuming you're using express - expressjs.com)
    // Get the credit card details submitted by the form
    var stripeToken = req.body.stripeToken;

    var charge = stripe.charges.create({
      amount: 1337, // amount in cents, again
      currency: "gbp",
      card: stripeToken,
      description: "payinguser@example.com"
    }, function(err, charge) {
      if (err && err.type === 'StripeCardError') {
        // The card has been declined
      }
      res.send(200, "Everything is ok: "+arguments.toString());
    });
});

module.exports = router;
