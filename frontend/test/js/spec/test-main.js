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
    baseUrl: '/base/assets/javascripts/',

    // Keep these in sync with the paths found in the requireJs paths
    paths: {
        '$': 'src/utils/$',
        'modernizr': 'lib/modernizr',
        'lodash': 'lib/bower-components/lodash-amd/modern',
        'bean': 'lib/bower-components/bean/bean',
        'bonzo': 'lib/bower-components/bonzo/bonzo',
        'qwery': 'lib/bower-components/qwery/qwery',
        'reqwest': 'lib/bower-components/reqwest/reqwest',
        'respimage': 'lib/bower-components/respimage/respimage',
        'lazySizes': 'lib/bower-components/lazysizes/lazysizes',
        'raven': 'lib/bower-components/raven-js/dist/raven',
        'ajax': 'src/utils/ajax'
    },

    // ask Require.js to load these files (all our tests)
    deps: tests,

    // start test run, once Require.js is done
    callback: window.__karma__.start
});
