var Uglify = require("webpack/lib/optimize/UglifyJsPlugin");
var path = require("path");

module.exports = function(debug) { return {
    resolve: {
        modules: [
            path.join(__dirname, "assets/javascripts"),
            path.join(__dirname, "node_modules/")
        ],
        extensions: [".js", ".es6"],
        alias: {
            '$$': 'src/utils/$',
            'lodash': 'lodash-amd',
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
        rules: [
            {
                test: /\.es6$/,
                exclude: /node_modules/,
                loader: 'babel-loader',
                query: {
                    presets: ['es2015'],
                    cacheDirectory: ''
                }
            }
        ]
    },

    resolveLoader: {
        modules: [path.join(__dirname, "node_modules")]
    },

    plugins: !debug ? [
        new Uglify(
            {
                sourceMap: true
            })
    ] : [],

    watch: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: 'assets/javascripts',
    devtool: 'source-map'
}};
