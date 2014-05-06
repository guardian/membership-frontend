/* global module: false, process: false */
module.exports = function (grunt) {
    'use strict';

    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';
    var pkg = grunt.file.readJSON('package.json');
    var singleRun = grunt.option('single-run') !== false;

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    // Project configuration..
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
                root: 'common/app/assets',
                stylesheets: '<%= dirs.assets.root %>/stylesheets',
                javascripts: '<%= dirs.assets.root %>/javascripts'
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

        requirejs: {
            compile: {
                options: {
                    include: ['main'],
                    baseUrl: '<%= dirs.assets.javascripts %>/src',
                    paths: {
                        '$': 'utils/$',
                        'bonzo': '../lib/bower-components/bonzo/bonzo',
                        'qwery': '../lib/bower-components/qwery/qwery',
                        'domready': '../lib/bower-components/domready/ready',
                        //'eventsForm': 'modules/events/forms',
                        'ctaButton': 'modules/events/ctaButton',
                        'user': 'utils/user',
                        'credentials': 'config/credentials',
                        'externalDependencies': 'config/externalDependencies'/*,
                        'stripePayment': 'lib/stripe/stripe.payment',
                        'stripe': 'lib/stripe/stripe.min'*/
                    },
                    findNestedDependencies: true,
                    wrapShim: true,
                    optimize: 'none',
                    generateSourceMaps: true,
                    preserveLicenseComments: true,
                    out: '<%= dirs.publicDir.javascripts %>/main.js'
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
            },
            require: {
                src: '<%= dirs.assets.javascripts %>/lib/bower-components/requirejs/require.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/requirejs/',
                expand: true,
                flatten: true
            }
        },


        // Clean stuff up
        clean: {
            js : ['<%= dirs.publicDir.javascripts %>'],
            css: ['<%= dirs.publicDir.stylesheets %>'],
            hooks: ['.git/hooks/pre-commit']
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

            unit: {
                configFile: 'karma.conf.js',
                browsers: ['PhantomJS']
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
                    cwd: '<%= dirs.assets.javascripts %>/',
                    src: ['**/*.js', '!**/lib/**/*.js', '!**/atob.js']
                }]
            }
        },

        // misc

        shell: {
            /**
             * Using this task to copy hooks, as Grunt's own copy task doesn't preserve permissions
             */
            copyHooks: {
                command: 'cp git-hooks/pre-commit .git/hooks/',
                options: {
                    stdout: true,
                    stderr: true,
                    failOnError: false
                }
            }
        }

    });

    // Load the plugins
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-karma');
    grunt.loadNpmTasks('grunt-shell');

    grunt.registerTask('compile', [
        'compile:css',
        'compile:js'
    ]);

    // Test tasks
    grunt.registerTask('test:unit', function() {
        grunt.config.set('karma.options.singleRun', (singleRun === false) ? false : true);
        grunt.task.run(['karma:unit']);
    });

    grunt.registerTask('test', ['jshint', 'test:unit']);

    grunt.registerTask('compile:css', ['clean:css', 'sass:compile']);

    grunt.registerTask('compile:js', ['clean:js', 'requirejs:compile', 'copy:require']);

    grunt.registerTask('hookup', ['clean:hooks'], ['shell:copyHooks']);

};