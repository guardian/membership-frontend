var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Membership prototypes' });
});

/* GET home page. */
router.get('/stripe', function(req, res) {
  res.render('stripe', { title: 'Stripe' });
});

module.exports = router;
