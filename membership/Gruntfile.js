/* global module: false, process: false */
module.exports = function (grunt) {
    'use strict';

    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';
    var pkg = grunt.file.readJSON('package.json');
    var testConfDir = './common/test/assets/javascripts/conf/';
    var singleRun = grunt.option('single-run') !== false;

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    // Project configuration.
    grunt.initConfig({

        pkg: pkg,

        dirs: {
            publicDir: {
                root: 'public',
                stylesheets: '<%= dirs.publicDir.root %>/stylesheets',
                javascripts: '<%= dirs.publicDir.root %>/javascripts',
                images: '<%= dirs.publicDir.root %>/images'
            },
            assets: {
                root: 'app/assets',
                stylesheets: '<%= dirs.assets.root %>/stylesheets'
            }
        },

        /***********************************************************************
         * Compile
         **********************************************************************/
        sass: {
            compile: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: ['*.scss', '!_*'],
                    dest: '<%= dirs.publicDir.stylesheets %>',
                    ext: '.css'
                }],
                options: {
                    style: 'compressed',
                    sourcemap: true,
                    noCache: true,
                    quiet: isDev ? false : true,
                    loadPath: [
                        '<%= dirs.assets.stylesheets %>/components/sass-mq'
                    ]
                }
            }
        },

        copy: {
            css: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: ['**/*.scss'],
                    dest: '<%= dirs.publicDir.stylesheets %>'
                }]
            }
        },


        // Clean stuff up
        clean: {
            css : ['<%= dirs.publicDir.stylesheets %>']
        },

        // Recompile on change
        watch: {
            css: {
                files: ['<%= dirs.assets.stylesheets %>/**/*.scss'],
                tasks: ['compile:css'],
                options: {
                    spawn: false
                }
            }
        },

        /***********************************************************************
         * Test
         **********************************************************************/

        karma: {
            options: {
                reporters: isDev ? ['dots'] : ['progress'],
                singleRun: singleRun
            },
            common: {
                configFile: testConfDir + 'common.js'
            },
            facia: {
                configFile: testConfDir + 'facia.js'
            }
        },

        // Lint Javascript sources
        jshint: {
            options: {
                jshintrc: 'jshint_conf.json'
            },
            self: [
                'Gruntfile.js'
            ],
            common: {
                files: [{
                    expand: true,
                    cwd: 'public/javascripts/',
                    src: ['**/*.js', '!**/components/**/*.js', '!**/atob.js']
                }]
            }
        }

        // Much of the CasperJS setup borrowed from smlgbl/grunt-casperjs-extra
//        env: {
//            casperjs: {
//                ENVIRONMENT : (process.env.ENVIRONMENT) ? process.env.ENVIRONMENT : (isDev) ? 'dev' : 'code',
//                PHANTOMJS_EXECUTABLE : 'node_modules/casperjs/node_modules/.bin/phantomjs',
//                extend: {
//                    PATH: {
//                        value: 'node_modules/.bin',
//                        delimiter: ':'
//                    }
//                }
//            }
//        },
//
//        casperjsLogFile: 'results.xml',
//        casperjs: {
//            options: {
//                casperjsOptions: [
//                    '--verbose',
//                    '--log-level=warning',
//                    '--ignore-ssl-errors=yes',
//                    '--includes=integration-tests/casper/tests/shared.js',
//                    '--xunit=integration-tests/target/casper/<%= casperjsLogFile %>'
//                ]
//            },
//            screenshot: {
//                src: ['tools/screenshots/screenshot.js']
//            },
//            all: {
//                src: ['integration-tests/casper/tests/**/*.spec.js']
//            },
//            admin: {
//                src: ['integration-tests/casper/tests/admin/*.spec.js']
//            },
//            article: {
//                src: ['integration-tests/casper/tests/article/*.spec.js']
//            },
//            applications: {
//                src: ['integration-tests/casper/tests/applications/*.spec.js']
//            },
//            common : {
//                src: ['integration-tests/casper/tests/common/*.spec.js']
//            },
//            discussion: {
//                src: ['integration-tests/casper/tests/discussion/*.spec.js']
//            },
//            facia: {
//                src: ['integration-tests/casper/tests/facia/*.spec.js']
//            },
//            identity: {
//                src: ['integration-tests/casper/tests/identity/*.spec.js']
//            },
//            open: {
//                src: ['integration-tests/casper/tests/open/*.spec.js']
//            },
//            commercial: {
//                src: ['integration-tests/casper/tests/commercial/*.spec.js']
//            }
//        }

    });

    // Load the plugins
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-jshint');

    grunt.registerTask('compile', [
        'compile:css'
    ]);

    // Test tasks
//    grunt.registerTask('test:integration', function(app) {
//        if (!app) {
//            grunt.fail.fatal('No app specified.');
//        }
//        // does a casperjs setup exist for this app
//        grunt.config.requires(['casperjs', app]);
//        grunt.config('casperjsLogFile', app + '.xml');
//        grunt.task.run(['env:casperjs', 'casperjs:' + app]);
//    });
    grunt.registerTask('test:unit', function(app) {
        var apps = [];
        // have we supplied an app
        if (app) {
            // does a karma setup exist for this app
            if (!grunt.config('karma')[app]) {
                grunt.log.warn('No tests for app "' + app + '"');
                return true;
            }
            apps = [app];
        } else { // otherwise run all
            apps = Object.keys(grunt.config('karma')).filter(function(app) { return app !== 'options'; });
        }
        grunt.config.set('karma.options.singleRun', (singleRun === false) ? false : true);
        apps.forEach(function(app) {
            grunt.task.run(['karma:' + app]);
        });
    });
    // TODO - don't have common as default?
    grunt.registerTask('test', ['jshint']);

    grunt.registerTask('compile:css', ['clean:css', 'sass:compile']);
};