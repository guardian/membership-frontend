define([
    'src/utils/cookie'
],
function (cookie) {

    var COOKIE_EXPIRES_DAYS = 1;
    var win = window;

    var Storage = function(type) {
        this.type = type;
        this.init();
    };

    Storage.prototype.storageIsAvailable = false;

    Storage.prototype.init = function() {

        this.storageIsAvailable = this.isAvailable();
    };

    Storage.prototype.isAvailable = function() {
        var testKey = 'local-storage-capability-test';
        var data = 'test';
        /**
         * Use LocalStorage if it is available if not fallback to cookies
         * this has been implemented because on Safari (OS X or IOS) in private mode we
         * don't have access to localStorage
         */
        try {
            win[this.type].setItem(testKey, data);
            win[this.type].removeItem(testKey);
            return true;
        } catch (e) {
            return false;
        }
    };

    Storage.prototype.set = function(key, data, options) {
        var opts = options || {};
        var value = JSON.stringify({
                'value': data,
                'expires': opts.expires && opts.expires
            });

        if (!this.storageIsAvailable) {
            cookie.setCookie(key, value, COOKIE_EXPIRES_DAYS);
        } else {
            win[this.type].setItem(key, value);
        }
    };

    Storage.prototype.get = function(key) {
        var dataParsed;
        var data = !this.storageIsAvailable ? cookie.getCookie(key) : win[this.type].getItem(key);

        if (data === null) {
            return null;
        }

        try{
            dataParsed = JSON.parse(data);
        } catch (e) {
            this.remove(key);
            return null;
        }

        // has it expired?
        if (dataParsed.expires && new Date() > new Date(dataParsed.expires)) {
            this.remove(key);
            return null;
        }

        return dataParsed.value;
    };

    Storage.prototype.remove = function(key) {
        if (!this.storageIsAvailable) {
            cookie.removeCookie(key);
        } else {
            win[this.type].removeItem(key);
        }
    };

    return {
        local: new Storage('localStorage'),
        session: new Storage('sessionStorage')
    };

});
