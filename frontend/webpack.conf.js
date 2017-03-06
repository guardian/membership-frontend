var Uglify = require("webpack/lib/optimize/UglifyJsPlugin");

module.exports = function(debug) { return {
    resolve: {
        root: ["assets/javascripts", "assets/../node_modules/"],
        extensions: ["", ".js", ".es6"],
        alias: {
            '$$': 'src/utils/$',
            'lodash': 'lodash-amd/modern',
            'bean': 'bean/bean',
            'bonzo': 'bonzo/bonzo',
            'qwery': 'qwery/qwery',
            'reqwest': 'reqwest/reqwest',
            'respimage': 'respimage/respimage',
            'lazySizes': 'lazysizes/lazysizes',
            'gumshoe': 'gumshoe/dist/js/gumshoe',
            'smoothScroll': 'smooth-scroll/dist/js/smooth-scroll',
            'ajax': 'src/utils/ajax',
            'URLSearchParams': 'url-search-params'
        }
    },

    module: {
        loaders: [
            {
                test: /\.es6$/,
                exclude: /node_modules/,
                loader: 'babel',
                query: {
                    presets: ['es2015'],
                    cacheDirectory: ''
                }
            }
        ]
    },

    plugins: !debug ? [
        new Uglify({compress: {warnings: false}})
    ] : [],

    progress: true,
    failOnError: true,
    watch: false,
    keepalive: false,
    inline: true,
    hot: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: 'assets/javascripts',
    debug: false,
    devtool: 'source-map'
}};
