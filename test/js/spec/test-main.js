var tests = [];

for (var file in window.__karma__.files) {
    if (window.__karma__.files.hasOwnProperty(file)) {
        if (/\.spec\.js$/.test(file)) {
            tests.push(file);
        }
    }
}

requirejs.config({
    // Karma serves files from '/base'
    baseUrl: '/base/common/app/assets/javascripts/src',

    paths: {
        'jquery': '//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min',
        'jQueryPayment': 'components/stripe/jquery.payment',
        'stripe': 'https://js.stripe.com/v2/?',
        'eventsForm': 'modules/events/forms',
        'user': 'utils/user',
        'config': 'config/config'
    },
    shim: {
        'payment': {
            deps: ['jquery']
        }
    },

    // ask Require.js to load these files (all our tests)
    deps: tests,

    // start test run, once Require.js is done
    callback: window.__karma__.start
});