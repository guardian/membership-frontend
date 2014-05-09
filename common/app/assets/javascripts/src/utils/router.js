define(function () {

    function Router () {
        this.routes = {};
    }

    Router.prototype.match = function (route) {
        this.route = route;

        return this;
    };

    Router.prototype.to = function (fn) {
        this.routes[this.route] = fn;
    };

    Router.prototype.go = function () {
        var path = window.location.pathname;

        if (this.routes['*']) {
            this.routes['*']();
            delete this.routes['*'];
        }

        for (var key in this.routes) {
            var regxp = new RegExp('^'+ key);

            if (path.match(regxp)) {
                this.routes[key]();
            }
        }
    };

    return new Router();

});
