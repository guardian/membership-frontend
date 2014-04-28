 var gcConfig = {
    sandbox: true,
    appId: 'G64TPE52R4W5QVHK6PZ3J1V6ZCE8024CZHKYHTGWWFD2WWSXSGJC0MH4F7XA0YGG',
    appSecret: 'WHTVP5ZRN6D9SVMCPYGVF1T5VEN1YMANM5NZ2PSZRGPQ9Q5Z53VRTXGX5P19DHYA',
    token: 'QKRD1S73S8WW9GX9JEJGHQSH6J2PJR66APJKVHXTW5VC74SKKGBEE5G0SZZ1HWSG',
    merchantId: '0JEP19YDA6'
};
var gocardless = require('gocardless')(gcConfig);

var express = require('express');
var router = express.Router();

var stripe = require('stripe')("***REMOVED***");

var displayMerchantDetail = function(){
    gocardless.merchant.getSelf(function(err, response, body) {
        console.log( JSON.parse(body), 'Go Cardless repsonse' );
    });
};

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
router.get('/subscribe', function(req, res) {

    displayMerchantDetail();

    res.render('subscribe', { title: 'Go Cardless subscription', message: 'Please enter your email for subscription' });
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
            email: req.body.email
        }
    });

    res.redirect(url);
});

router.post('/bill', function(req, res){

    var url = gocardless.bill.newUrl({
        amount: '10.00',
        name: 'Coffee',
        description: 'One bag of single origin coffee'
    });

    res.redirect(url);
});

router.get('/bill', function(req, res, next){

    displayMerchantDetail();
    res.render('bill', { title: 'Go Cardless bill', message: 'Please enter your email for billing' });
});

router.get('/success', function(req, res){

    // Check the signature and POST back to GoCardless
    gocardless.confirmResource(req.query, function(err, request, body){

        if (err){
            return res.end(401, err);
        }
    
        res.render('success', { title: 'Go Cardless success', message: 'You are subscribed' });
    });
});

module.exports = router;
