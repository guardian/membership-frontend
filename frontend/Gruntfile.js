/* global module: false, process: false */
module.exports = function (grunt) {
    'use strict';

    require('time-grunt')(grunt);

    /**
     * Setup
     */
    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';
    var pkg = grunt.file.readJSON('package.json');
    var singleRun = grunt.option('single-run') !== false;

    /**
     * Load all grunt-* tasks
     */
    require('load-grunt-tasks')(grunt);

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    /**
     * Project configuration
     */
    grunt.initConfig({

        pkg: pkg,

        dirs: {
            publicDir: {
                root: 'public',
                stylesheets: '<%= dirs.publicDir.root %>/stylesheets',
                // bookmarklets are js files but we give them their own directory
                // to exclude them from asset hashing
                bookmarklets: '<%= dirs.publicDir.root %>/bookmarklets',
                javascripts: '<%= dirs.publicDir.root %>/javascripts',
                images: '<%= dirs.publicDir.root %>/images'
            },
            assets: {
                root: 'assets',
                stylesheets: '<%= dirs.assets.root %>/stylesheets',
                bookmarklets: '<%= dirs.assets.root %>/bookmarklets',
                javascripts: '<%= dirs.assets.root %>/javascripts',
                images: '<%= dirs.assets.root %>/images'
            }
        },

        /***********************************************************************
         * Compile
         ***********************************************************************/

        sass: {
            options: {
                outputStyle: 'compressed',
                sourceMap: isDev,
                precision: 5
            },
            dist: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: [
                        'style.scss',
                        'ie9.style.scss',
                        'tools.style.scss',
                        'event-card.scss'
                    ],
                    dest: '<%= dirs.publicDir.stylesheets %>',
                    ext: '.css'
                }]
            }
        },

        px_to_rem: {
            dist: {
                options: {
                    map: isDev,
                    base: 16,
                    fallback: false,
                    max_decimals: 5
                },
                files: [{
                    expand: true,
                    cwd: '<%= dirs.publicDir.stylesheets %>',
                    src: ['*.css'],
                    dest: '<%= dirs.publicDir.stylesheets %>'
                }]
            }
        },

        postcss: {
            options: {
                map: isDev ? true : false,
                processors: [
                    require('autoprefixer-core')({browsers: ['> 5%', 'last 2 versions', 'IE 9', 'Safari 6']})
                ]
            },
            dist: { src: '<%= dirs.publicDir.stylesheets %>/*.css' }
        },

        requirejs: {
            compile: {
                options: {
                    name: 'src/main',
                    baseUrl: '<%= dirs.assets.javascripts %>',
                    // Keep these in sync with the paths found in the karma test-main.js paths
                    paths: {
                        '$': 'src/utils/$',
                        'lodash': 'lib/bower-components/lodash-amd/modern',
                        'bean': 'lib/bower-components/bean/bean',
                        'bonzo': 'lib/bower-components/bonzo/bonzo',
                        'qwery': 'lib/bower-components/qwery/qwery',
                        'reqwest': 'lib/bower-components/reqwest/reqwest',
                        'respimage': 'lib/bower-components/respimage/respimage',
                        'lazySizes': 'lib/bower-components/lazysizes/lazysizes',
                        'raven': 'lib/bower-components/raven-js/dist/raven',
                        'gumshoe': 'lib/bower-components/gumshoe/dist/js/gumshoe',
                        'smoothScroll': 'lib/bower-components/smooth-scroll/dist/js/smooth-scroll',
                        'ajax': 'src/utils/ajax',
                        'text':'lib/bower-components/requirejs-text/text'
                    },
                    findNestedDependencies: false,
                    wrapShim: true,
                    optimize: isDev ? 'none' : 'uglify2',
                    generateSourceMaps: true,
                    preserveLicenseComments: false,
                    out: '<%= dirs.publicDir.javascripts %>/main.js'
                }
            },
            compileTools: {
                options: {
                    name: 'src/tools',
                    baseUrl: '<%= dirs.assets.javascripts %>',
                    findNestedDependencies: false,
                    wrapShim: true,
                    optimize: isDev ? 'none' : 'uglify2',
                    generateSourceMaps: true,
                    preserveLicenseComments: false,
                    out: '<%= dirs.publicDir.javascripts %>/tools.js'
                }
            }
        },

        /***********************************************************************
         * Copy & Clean
         ***********************************************************************/

        copy: {
            css: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: ['**/*.scss'],
                    dest: '<%= dirs.publicDir.stylesheets %>'
                }]
            },
            polyfills: {
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
                src: '<%= dirs.assets.javascripts %>/lib/bower-components/zxcvbn/dist/zxcvbn.js',
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
            uet: {
                src: '<%= dirs.assets.javascripts %>/lib/analytics/uet.js',
                dest: '<%= dirs.publicDir.javascripts %>/lib/uet/',
                expand: true,
                flatten: true
            },
            // For developer use. These are js files, not served with any page
            // but included dynamically when clicking on the respective bookmarklet.
            bookmarklets: {
                cwd: '<%= dirs.assets.bookmarklets %>',
                src: ['*.js'],
                dest: '<%= dirs.publicDir.bookmarklets %>',
                expand: true
            },
            images: {
                cwd: '<%= dirs.assets.images %>',
                src: [
                    '**',
                    '!**/inline-svgs/raw/**'
                ],
                dest: '<%= dirs.publicDir.images %>',
                expand: true
            }
        },

        clean: {
            bookmarklets: ['<%= dirs.publicDir.bookmarklets %>'],
            js: ['<%= dirs.publicDir.javascripts %>'],
            css: ['<%= dirs.publicDir.stylesheets %>'],
            icons: ['<%= dirs.assets.images %>/inline-svgs/*.svg'],
            assetMap: 'conf/assets.map',
            images: ['<%= dirs.publicDir.images %>'],
            dist: ['<%= dirs.publicDir.root %>/dist/']
        },

        /***********************************************************************
         * Watch
         ***********************************************************************/

        watch: {
            compile_css: {
                files: ['<%= dirs.assets.stylesheets %>/**/*.scss'],
                tasks: ['compile:css']
            },
            compile_js: {
                files: ['<%= dirs.assets.javascripts %>/**/*.js'],
                tasks: ['compile:js']
            },
            compile_images: {
                files: ['<%= dirs.assets.images %>/**/*'],
                tasks: ['compile:images']
            }
        },

        /***********************************************************************
         * Assets
         ***********************************************************************/

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
            staticfiles: {
                files: [{
                    src: [
                        '<%= dirs.publicDir.stylesheets %>/**/*.css',
                        '<%= dirs.publicDir.javascripts %>/**/*.js',
                        '<%= dirs.publicDir.javascripts %>/**/*.map',
                        '<%= dirs.publicDir.images %>/**/*.*'
                    ],
                    dest: '<%= dirs.publicDir.root %>/dist/'
                }]
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
        },

        /***********************************************************************
         * Test & Validate
         ***********************************************************************/

        karma: {
            options: {
                reporters: ['progress'],
                singleRun: singleRun
            },
            unit: {
                configFile: 'karma.conf.js',
                browsers: ['PhantomJS']
            }
        },

        /**
         * Lint Javascript sources
         */
        eslint: {
            options: {
                configFile: '../.eslintrc'
            },
            app: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.javascripts %>/',
                    src: [
                        'config/**/*.js',
                        'src/**/*.js'
                    ]
                }]
            }
        },

        /***********************************************************************
         * Icons
         ***********************************************************************/

        svgmin: {
            options: {
                plugins: [
                    { removeViewBox: false },
                    { removeUselessStrokeAndFill: false },
                    { removeEmptyAttrs: false },
                    { cleanUpIds: false }
                ]
            },
            dist: {
                expand: true,
                cwd: '<%= dirs.assets.images %>/inline-svgs/raw',
                src: ['*.svg'],
                dest: '<%= dirs.assets.images %>/inline-svgs'
            }
        },

        svgstore: {
            options: {
                prefix: 'icon-',
                symbol: true,
                inheritviewbox: true,
                cleanup: ['fill'],
                svg: {
                    id: 'svg-sprite',
                    width: 0,
                    height: 0
                }
            },
            icons: {
                files: {
                    '<%= dirs.assets.images %>/svg-sprite.svg': ['<%= dirs.assets.images %>/inline-svgs/*.svg']
                }
            }
        }

    });


    /***********************************************************************
     * Compile & Validate
     ***********************************************************************/

    grunt.registerTask('validate', ['eslint']);

    grunt.registerTask('build:images', ['svgSprite', 'copy:images', 'imagemin']);
    grunt.registerTask('build:css', ['sass', 'px_to_rem', 'postcss']);

    grunt.registerTask('compile:css', [
        'clean:css',
        'build:css'
    ]);
    grunt.registerTask('compile:bookmarklets', [
        'clean:bookmarklets',
        'copy:bookmarklets'
    ]);
    grunt.registerTask('compile:images', [
        'clean:images',
        'build:images'
    ]);
    grunt.registerTask('compile:js', function() {
        if (!isDev) {
            grunt.task.run(['validate']);
        }
        grunt.task.run([
            'clean:js',
            'requirejs:compile',
            'requirejs:compileTools',
            'copy:polyfills',
            'copy:curl',
            'copy:zxcvbn',
            'copy:omniture',
            'copy:uet'
        ]);
    });
    grunt.registerTask('compile', function(){
        grunt.task.run([
            'clean:public',
            'compile:css',
            'compile:images',
            'compile:js',
            'compile:bookmarklets'
        ]);
        /**
         * Only version files for prod builds
         * Wipe out unused non-versioned assets for good measure
         */
        if (!isDev) {
            grunt.task.run([
                'asset_hash',
                'clean:public:prod'
            ]);
        }
    });

    /***********************************************************************
     * Test
     ***********************************************************************/

    grunt.registerTask('test', function(){
        grunt.task.run(['test:unit']);
    });
    grunt.registerTask('test:unit', function() {
        grunt.config.set('karma.options.singleRun', (singleRun === false) ? false : true);
        grunt.task.run(['karma:unit']);
    });

    /***********************************************************************
     * Icons
     ***********************************************************************/

    grunt.registerTask('svgSprite', ['clean:icons', 'svgmin', 'svgstore']);

    /***********************************************************************
     * Clean
     ***********************************************************************/

    grunt.registerTask('clean:public', [
        'clean:bookmarklets',
        'clean:js',
        'clean:css',
        'clean:images',
        'clean:assetMap',
        'clean:dist'
    ]);
    // Why don't we clean bookmarklets here? Because this is for cleaning out
    // the pre-hashed assets after hashing, and bookmarklets aren't hashed.
    grunt.registerTask('clean:public:prod', [
        'clean:js',
        'clean:css',
        'clean:images'
    ]);
};
