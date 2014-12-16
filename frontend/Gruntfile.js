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
                images:      '<%= dirs.publicDir.root %>/images'
            },
            assets: {
                root: 'assets',
                stylesheets: '<%= dirs.assets.root %>/stylesheets',
                javascripts: '<%= dirs.assets.root %>/javascripts',
                images:      '<%= dirs.assets.root %>/images'
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
                    sourcemap: isDev ? true : false,
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
                    baseUrl: '<%= dirs.assets.javascripts %>',
                    paths: {
                        '$': 'src/utils/$',
                        'bean': 'lib/bower-components/bean/bean',
                        'bonzo': 'lib/bower-components/bonzo/bonzo',
                        'qwery': 'lib/bower-components/qwery/qwery',
                        'reqwest': 'lib/bower-components/reqwest/reqwest',
                        'ajax': 'src/utils/ajax',
                        'stripe': 'lib/stripe/stripe.min'
                    },
                    findNestedDependencies: false,
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
            pollyfils: {
                src: '<%= dirs.assets.javascripts %>/lib/polyfills.min.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/polyfills.min.js'
            },
            curl: {
                src: '<%= dirs.assets.javascripts %>/lib/bower-components/curl/dist/curl-with-js-and-domReady/curl.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/curl/',
                expand: true,
                flatten: true
            },
            zxcvbn: {
                src: '<%= dirs.assets.javascripts %>/lib/bower-components/zxcvbn/zxcvbn.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/zxcvbn/',
                expand: true,
                flatten: true
            },
            omniture: {
                src: '<%= dirs.assets.javascripts %>/lib/analytics/omniture.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/omniture/',
                expand: true,
                flatten: true
            },
            zeroclipboard: {
                src: [
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.min.js',
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.min.map',
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.swf'
                ],
                dest: '<%= dirs.publicDir.javascripts %>/lib/zeroclipboard/',
                expand: true,
                flatten: true
            },
            //TODO-ben zeroclipboardDist is a stop gap until we have the ability to hash folders and their contents
            zeroclipboardDist: {
                src: [
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.min.js',
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.min.map',
                    '<%= dirs.assets.javascripts %>/lib/bower-components/zeroclipboard/dist/ZeroClipboard.swf'
                ],
                dest: '<%= dirs.publicDir.javascripts %>/lib/zeroclipboard/',
                expand: true,
                flatten: true
            },
            images: {
                cwd: '<%= dirs.assets.images %>',
                src: ['**', '!**/svgs/**'],
                dest: '<%= dirs.publicDir.images %>',
                expand: true
            },
            imagesNoAssetHash: {
                cwd: '<%= dirs.assets.images %>',
                src: ['**/noAssetHash/**'],
                dest: '<%= dirs.publicDir.root %>/images/',
                expand: true
            }
        },


        // Clean stuff up
        clean: {
            js : ['<%= dirs.publicDir.javascripts %>'],
            css: ['<%= dirs.publicDir.stylesheets %>'],
            assetMap: 'conf/assets.map',
            hooks: {
                src: ['../.git/hooks/pre-commit'],
                options: {
                    force: true // or we can't delete outside cwd
                }
            },
            images: ['<%= dirs.publicDir.images %>'],
            dist: ['<%= dirs.publicDir.root %>/dist/']
        },

        // Recompile on change
        watch: {
            css: {
                files: ['<%= dirs.assets.stylesheets %>/**/*.scss'],
                tasks: ['compile:css'],
                options: {
                    spawn: false,
                    livereload: true
                }
            },
            js: {
                files: ['<%= dirs.assets.javascripts %>/**/*.js'],
                tasks: ['compile:js'],
                options: {
                    spawn: false,
                    livereload: true
                }
            }
        },

        // generate a mapping file of hashed assets
        // and move/rename built files to /dist/
        asset_hash: {
            options: {
                preserveSourceMaps: true,
                assetMap: isDev ? false : 'conf/assets.map',
                hashLength: 8,
                algorithm: 'md5',
                srcBasePath: 'public/',
                destBasePath: 'public/',
                references: [
                    '<%= dirs.publicDir.root %>/dist/stylesheets/**/*.css'
                ]
            },
            //TODO-ben ignoring noAssetHash and zeroclipboard is a stop gap until we have the ability to hash folders and their contents
            staticfiles: {
                files: [
                    {
                        src:  [
                            '<%= dirs.publicDir.stylesheets %>/**/*.css',
                            '<%= dirs.publicDir.javascripts %>/**/*.js',
                            '<%= dirs.publicDir.javascripts %>/**/*.map',
                            '<%= dirs.publicDir.images %>/**/*.*',
                            '!<%= dirs.publicDir.images %>/noAssetHash/**/*.*',
                            '!<%= dirs.publicDir.javascripts %>/lib/zeroclipboard/**/*.*'
                        ],
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
            svgencode: {
                command: 'find <%= dirs.assets.images %>/svgs -name \\*.svg | python svgencode.py icon > <%= dirs.assets.stylesheets %>/icons/icons.scss'
            },
            /**
             * Using this task to copy hooks, as Grunt's own copy task doesn't preserve permissions
             */
            copyHooks: {
                command: 'cp ../git-hooks/pre-commit ../.git/hooks/',
                options: {
                    stdout: true,
                    stderr: true,
                    failOnError: true
                }
            }
        },

        imagemin: {
            dynamic: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.publicDir.images %>',
                    src: ['**/*.{png,jpg}', '!**/svgs/**'],
                    dest: '<%= dirs.publicDir.images %>'
                }]
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
    grunt.loadNpmTasks('grunt-scss-lint');
    grunt.loadNpmTasks('grunt-karma');
    grunt.loadNpmTasks('grunt-shell');
    grunt.loadNpmTasks('grunt-asset-hash');
    grunt.loadNpmTasks('grunt-contrib-imagemin');

    grunt.registerTask('compile', function(){
        grunt.task.run([
            'clean:public',
            'compile:css',
            'compile:js'
        ]);
        // only version files for prod builds
        // and wipe out unused non-versioned assets for good measure
        // Copy noAssetHash folder to dist
        if (!isDev) {
            grunt.task.run([
                'asset_hash',
                'clean:public:prod',
                'copy:imagesNoAssetHash',
                 'copy:zeroclipboardDist'
            ]);
        }
    });

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

    grunt.registerTask('clean:public', ['clean:js', 'clean:css', 'clean:images', 'clean:assetMap', 'clean:dist']);
    grunt.registerTask('clean:public:prod', ['clean:js', 'clean:css', 'clean:images']);

    grunt.registerTask('compile:css', function() {
        if (!isDev) {
            grunt.task.run(['scsslint']);
        }
        grunt.task.run([
            'clean:css',
            'clean:images',
            'copy:images',
            'shell:svgencode',
            'sass:compile',
            'imagemin'
        ]);
    });

    grunt.registerTask('compile:js', function() {
        if (!isDev) {
            grunt.task.run(['jshint']);
        }
        grunt.task.run([
            'clean:js',
            'requirejs:compile',
            'copy:pollyfils',
            'copy:curl',
            'copy:zxcvbn',
            'copy:omniture',
            'copy:zeroclipboard'
        ]);
    });

    grunt.registerTask('hookup', ['clean:hooks'], ['shell:copyHooks']);
};
