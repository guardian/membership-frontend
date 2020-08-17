var tests = [];

for (var file in window.__karma__.files) {
    if (window.__karma__.files.hasOwnProperty(file)) {
        if (/\.spec\.js$/.test(file)) {
            tests.push(file);
        }
    }
}

requirejs.config({
    /**
     * Karma serves files from '/base'
     */
    baseUrl: '/base/assets/javascripts/',

    /**
     * Keep these in sync with the paths
     * found in the requirejs Grunt config
     * in Gruntfile.js
     */
    //
    paths: {
        'modernizr': 'lib/modernizr',
        //'lodash': 'lib/bower-components/lodash-amd/modern',
        'bean': 'lib/bower-components/bean/bean',
        'reqwest': 'lib/bower-components/reqwest/reqwest',
        'respimage': 'lib/bower-components/respimage/respimage',
        'lazySizes': 'lib/bower-components/lazysizes/lazysizes',
        'raven': 'lib/bower-components/raven-js/dist/raven',
        'smoothScroll': 'lib/bower-components/smooth-scroll/dist/js/smooth-scroll',
        'ajax': 'src/utils/ajax'
    },

    /**
     * Ask require.js to load these files (all our tests)
     */
    deps: tests,

    /**
     * Start test run, once require.js is done
     */
    callback: window.__karma__.start
});
