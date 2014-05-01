// Karma configuration
// Generated on Fri Apr 25 2014 12:10:07 GMT+0100 (BST)

//module.exports = function(config) {
//    return {
//        // root of project
//        basePath: './../../../../../',
//        frameworks: ['jasmine', 'requirejs'],
//
//        files: [
//            { pattern: 'common/test/assets/javascripts/components/sinonjs/sinon.js', included: true },
//            { pattern: 'common/test/assets/javascripts/components/jasmine-sinon/lib/jasmine-sinon.js', included: true },
//            { pattern: 'common/test/assets/javascripts/components/seedrandom/index.js', included: true },
//            { pattern: 'common/test/assets/javascripts/setup.js', included: true },
//            { pattern: 'common/test/assets/javascripts/main.js', included: true },
//            { pattern: 'common/test/assets/javascripts/components/**/!(*.spec.js)', included: false },
//            { pattern: 'common/test/assets/javascripts/fixtures/**/*', included: false },
//            { pattern: 'common/test/assets/javascripts/helpers/**/*.js', included: false },
//            { pattern: 'common/test/assets/javascripts/spies/**/*.js', included: false },
//            { pattern: 'common/app/assets/javascripts/**/*.js', included: false }
//        ],
//
//        exclude: [],
//
//        // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
//        reporters: ['progress'],
//        port: 9876,
//        colors: true,
//        // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
//        logLevel: config.LOG_ERROR,
//        autoWatch: true,
//
//        // Start these browsers, currently available:
//        // - Chrome
//        // - ChromeCanary
//        // - Firefox
//        // - Opera (has to be installed with `npm install karma-opera-launcher`)
//        // - Safari (only Mac; has to be installed with `npm install karma-safari-launcher`)
//        // - PhantomJS
//        // - IE (only Windows; has to be installed with `npm install karma-ie-launcher`)
//        browsers: ['PhantomJS'],
//        captureTimeout: 60000,
//        singleRun: false
//    };
//};
//
//
//module.exports = function(config) {
//    var settings = new require('./settings.js')(config);
//    settings.files.push(
//        { pattern: 'common/test/assets/javascripts/spec/**/*.spec.js', included: false }
//    );
//    settings.app = 'common';
//    config.set(settings);
//}


module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine', 'requirejs'],


    // list of files / patterns to load in the browser
    files: [
        {pattern: 'public/javascripts/**/*.js', included: false},
        {pattern: 'test/js/spec/*.spec.js', included: false},
        'test/js/spec/test-main.js'
    ],


    // list of files to exclude
    exclude: [
        'public/javascripts/main.js'
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
    
    },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Chrome', 'PhantomJS'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false
  });
};
