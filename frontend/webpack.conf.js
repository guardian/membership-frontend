const TerserPlugin = require('terser-webpack-plugin');
var LoaderOptions = require("webpack/lib/LoaderOptionsPlugin");
var path = require("path");

var prodPlugins = [ new TerserPlugin({sourceMap: true}), new LoaderOptions({ minimize: true, debug: false })];
var debugPlugins = [new LoaderOptions({ debug: true })];

module.exports = function(debug) { return {
    resolve: {
        modules: [
            path.join(__dirname, "assets/javascripts"),
            path.join(__dirname, "node_modules/")
        ],
        extensions: [".js", ".es6"],
        alias: {
            '$$': 'jquery/dist/jquery.min',
            'lodash': 'lodash-amd',
            'respimage': 'respimage/respimage',
            'lazySizes': 'lazysizes/lazysizes',
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
                    cacheDirectory: '',
                    plugins: ['transform-object-rest-spread']
                }
            }
        ]
    },

    resolveLoader: {
        modules: [path.join(__dirname, "node_modules")]
    },

    plugins: !debug ? prodPlugins :debugPlugins,

    mode: debug ? 'development' : 'production',

    watch: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: path.join(__dirname, 'assets/javascripts'),
    devtool: 'source-map'
}};
