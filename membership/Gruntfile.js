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

        copy: {
            css: {
                files: [{
                    expand: true,
                    cwd: '<%= dirs.assets.stylesheets %>',
                    src: ['**/*.scss'],
                    dest: '<%= dirs.public.stylesheets %>'
                }]
            }
        },


        // Clean stuff up
        clean: {
            css : ['<%= dirs.public.stylesheets %>']
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

    grunt.registerTask('compile', [
        'compile:css'
    ]);

    grunt.registerTask('compile:css', ['clean:css', 'sass:compile']);
};