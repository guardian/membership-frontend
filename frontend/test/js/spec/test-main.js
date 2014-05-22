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
    baseUrl: '/base/common/app/assets/javascripts/',

    paths: {
        '$': 'src/utils/$',
        'bean': 'lib/bower-components/bean/bean',
        'bonzo': 'lib/bower-components/bonzo/bonzo',
        'qwery': 'lib/bower-components/qwery/qwery',
        'domready': 'lib/bower-components/domready/ready',
        'stripe': 'lib/stripe/stripe.min',
        'reqwest': 'lib/bower-components/reqwest/reqwest',
        'ajax': 'src/utils/ajax'
    },

    // ask Require.js to load these files (all our tests)
    deps: tests,

    // start test run, once Require.js is done
    callback: window.__karma__.start
});