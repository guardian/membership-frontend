/* global module: false, process: false */
module.exports = function (grunt) {
    'use strict';

    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';
    var pkg = grunt.file.readJSON('package.json');

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    // Project configuration.
    grunt.initConfig({

        pkg: pkg,

        dirs: {
            public: {
                root: 'public',
                stylesheets: '<%= dirs.public.root %>/stylesheets',
                javascripts: '<%= dirs.public.root %>/javascripts',
                images: '<%= dirs.public.root %>/images'
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
                    dest: '<%= dirs.public.stylesheets %>',
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
                    baseUrl: 'common/app/assets/javascripts',
                    paths: {
                        '$': '$',
                        'bonzo': 'components/bonzo/bonzo',
                        'qwery': 'components/qwery/qwery',
                        'eventsForm': 'modules/events/forms',
                        'ctaButton': 'modules/events/ctaButton',
                        'user': 'utils/user',
                        'credentials': 'config/credentials',
                        'externalDependencies': 'config/externalDependencies',
                        'jQueryPayment': 'components/stripe/jquery.payment',
                        'jquery': 'empty:',
                        'stripe': 'empty:'
                    },
                    shim: {
                        'jQueryPayment': {
                            deps: ['jquery']
                        }
                    },
                    optimize: 'none',
                    generateSourceMaps: true,
                    preserveLicenseComments: true,
                    out: 'public/javascripts/main.js'
                }
            }
        },

        copy: {
            css: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: ['**/*.scss'],
                    dest: '<%= dirs.public.stylesheets %>'
                }]
            },
            require: {
                src: '<%= dirs.assets.javascripts %>/components/requirejs/require.js',
                dest: '<%= dirs.public.javascripts %>/libs/requirejs/',
                expand: true,
                flatten: true
            }
        },


        // Clean stuff up
        clean: {
            css : ['<%= dirs.public.stylesheets %>'],
            js : ['<%= dirs.public.javascripts %>']
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
        }
    });

    // Load the plugins
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerTask('compile', [
        'compile:css',
        'compile:js'
    ]);

    grunt.registerTask('compile:css', ['clean:css', 'sass:compile']);
    grunt.registerTask('compile:js', ['clean:js', 'requirejs:compile', 'copy:require']);
};