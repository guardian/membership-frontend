var APP_ID      = 'G64TPE52R4W5QVHK6PZ3J1V6ZCE8024CZHKYHTGWWFD2WWSXSGJC0MH4F7XA0YGG';
var APP_SECRET  = 'WHTVP5ZRN6D9SVMCPYGVF1T5VEN1YMANM5NZ2PSZRGPQ9Q5Z53VRTXGX5P19DHYA';
var TOKEN       = 'QKRD1S73S8WW9GX9JEJGHQSH6J2PJR66APJKVHXTW5VC74SKKGBEE5G0SZZ1HWSG';
var MERCHANT_ID = '0JEP19YDA6';

// go cardless config
var gcConfig = {
    sandbox: true,
    appId: APP_ID,
    appSecret: APP_SECRET,
    token: TOKEN,
    merchantId: MERCHANT_ID
};
var gocardless = require('gocardless')(gcConfig);

var express = require('express');
var router = express.Router();

var stripe = require('stripe')("***REMOVED***");

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Membership prototypes' });
});

/* GET home page. */
router.get('/stripe', function(req, res) {
  res.render('stripe', { title: 'Stripe' , message: 'Please submit payment'});
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
        description: "chris.finch@theguardian.com"
    }, function(err, charge) {
        if (err && err.type === 'StripeCardError') {
            // The card has been declined
            res.render('stripe', { title: 'Stripe' , message: 'Error: ' + err.type});
        }
        res.render('stripe', { title: 'Stripe' , message: 'Payment successfull'});
    });
});

/* GET go cardless */
router.get('/go-cardless', function(req, res) {

    gocardless.merchant.getSelf(function(err, response, body) {
        console.log( JSON.parse(body), 'Go Cardless repsonse' );
    });

  res.render('go-cardless', { title: 'Go Cardless' });
});

/* POST subscribe go cardless */
router.post('/subscribe', function(req, res) {
    var url = gocardless.subscription.newUrl({
        amount: '15.00',
        interval_unit: 'month',
        interval_length: '1',
        name: 'Premium subscription',
        description: 'test description',
        user: {
            email: req.params.email
        }
    });

    res.redirect(url);
});

module.exports = router;
