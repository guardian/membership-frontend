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
                root: 'assets',
                stylesheets: '<%= dirs.assets.root %>/stylesheets',
                javascripts: '<%= dirs.assets.root %>/javascripts'
            }
        },

        /***********************************************************************
         * Compile
         ***********************************************************************/
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
                    name: 'src/main',
                    include: [
                        'requireLib'
                    ],
                    baseUrl: '<%= dirs.assets.javascripts %>',
                    paths: {
                        'requireLib': 'lib/bower-components/requirejs/require',
                        '$': 'src/utils/$',
                        'bean': 'lib/bower-components/bean/bean',
                        'bonzo': 'lib/bower-components/bonzo/bonzo',
                        'qwery': 'lib/bower-components/qwery/qwery',
                        'domready': 'lib/bower-components/domready/ready',
                        'reqwest': 'lib/bower-components/reqwest/reqwest',
                        'ajax': 'src/utils/ajax',
                        'stripe': 'lib/stripe/stripe.min',
                        'omniture': 'lib/analytics/omniture'
                    },
                    findNestedDependencies: true,
                    wrapShim: true,
                    optimize: isDev ? 'none' : 'uglify2',
                    generateSourceMaps: true,
                    preserveLicenseComments: false,
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
            html5shiv: {
                src: '<%= dirs.assets.javascripts %>/lib/bower-components/html5shiv/dist/html5shiv.min.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/html5shiv/',
                expand: true,
                flatten: true
            }
        },


        // Clean stuff up
        clean: {
            js : ['<%= dirs.publicDir.javascripts %>'],
            css: ['<%= dirs.publicDir.stylesheets %>'],
            dist: ['<%= dirs.publicDir.root %>/dist/'],
            assetMap: 'conf/assets.map',
            hooks: ['../.git/hooks/pre-commit']
        },

        // Recompile on change
        watch: {
            css: {
                files: ['<%= dirs.assets.stylesheets %>/**/*.scss'],
                tasks: ['compile:css'],
                options: {
                    spawn: false
                }
            },
            js: {
                files: ['<%= dirs.assets.javascripts %>/**/*.js'],
                tasks: ['compile:js'],
                options: {
                    spawn: false
                }
            }
        },

        // generate a mapping file of hashed assets
        // and move/rename built files to /dist/
        asset_hash: {
            options: {
                preserveSourceMaps: false,
                assetMap: isDev ? false : 'conf/assets.map',
                hashLength: 8,
                algorithm: 'md5',
                srcBasePath: 'public/',
                destBasePath: 'public/',
                hashType: 'file'
            },
            staticfiles: {
                files: [
                    {
                        src:  ['<%= dirs.publicDir.stylesheets %>/**/*.css', '<%= dirs.publicDir.javascripts %>/**/*.js'],
                        dest: '<%= dirs.publicDir.root %>/dist/'
                    }
                ]
            }
          },

        /***********************************************************************
         * Test
         ***********************************************************************/

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

        /***********************************************************************
         * Validate
         ***********************************************************************/

        // Lint Javascript sources
        jshint: {
            options: {
                jshintrc: '../jshint_conf.json'
            },
            self: [
                'Gruntfile.js'
            ],
            common: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.javascripts %>/',
                    src: [
                        '**/*.js',
                        '!**/lib/**/*.js',
                        '!**/atob.js',
                        '!**/user.js',
                        '!**/utils/analytics/omniture.js'
                    ]
                }]
            }
        },

        // Lint Sass sources
        scsslint: {
            allFiles: [
                '<%= dirs.assets.stylesheets %>/**/*.scss',
                '!<%= dirs.assets.stylesheets %>/components/bower-components/**/*.scss'
            ],
            options: {
                bundleExec: true,
                config: '.scss-lint.yml',
                reporterOutput: null
            }
        },

        // misc

        shell: {
            //  shamelessly stolen from NGW's gruntfile
            spriteGeneration: {
                command: [
                    'cd ../tools/sprites/',
                    'find . -name \'*.json\' -exec node spricon.js {} \\;'
                ].join('&&'),
                options: {
                    stdout: true,
                    stderr: true,
                    failOnError: true
                }
            },
            /**
             * Using this task to copy hooks, as Grunt's own copy task doesn't preserve permissions
             */
            copyHooks: {
                command: 'cp ../git-hooks/pre-commit ../.git/hooks/',
                options: {
                    stdout: true,
                    stderr: true,
                    failOnError: false
                }
            }
        }

    });

    // Load the plugins
    grunt.loadNpmTasks('grunt-asset-hash');
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-scss-lint');
    grunt.loadNpmTasks('grunt-karma');
    grunt.loadNpmTasks('grunt-shell');

    grunt.registerTask('compile', [
        'compile:css',
        'compile:js'
    ]);

    grunt.registerTask('validate', ['jshint', 'scsslint']);

    // Test tasks
    grunt.registerTask('test:unit', function() {
        grunt.config.set('karma.options.singleRun', (singleRun === false) ? false : true);
        grunt.task.run(['karma:unit']);
    });

    grunt.registerTask('test', function(){
        if (!isDev) {
            grunt.task.run(['validate']);
        }
        grunt.task.run(['test:unit']);
    });

    grunt.registerTask('clean-assets', ['clean:dist', 'clean:assetMap']);

    grunt.registerTask('compile:css', [
        'clean:css',
        'clean-assets',
        'shell:spriteGeneration',
        'scsslint',
        'sass:compile',
        'asset_hash'
    ]);

    grunt.registerTask('compile:js', function() {
        if (!isDev) {
            grunt.task.run(['jshint']);
        }
        grunt.task.run(['clean:js', 'clean-assets', 'requirejs:compile', 'copy:html5shiv', 'asset_hash']);
    });

    grunt.registerTask('hookup', ['clean:hooks'], ['shell:copyHooks']);
};
