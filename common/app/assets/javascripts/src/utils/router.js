define(function () {

    function Router () {
        this.routes = {};
    }

    Router.prototype.match = function (route) {
        this.route = route;

        return this;
    };

    Router.prototype.to = function (fn) {
        var self = this;
        if (typeof self.route === 'string') {
            self.routes[self.route] = fn;
        } else {
            self.route.forEach(function (e) {
                self.routes[e] = fn;
            });
        }
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
