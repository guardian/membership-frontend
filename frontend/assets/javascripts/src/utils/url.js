define(function() {
    'use strict';

    return {
        isExternal: function(url) {
            var external = url.replace('http://', '').replace('https://', '').split('/')[0];
            return (external.length) ? true : false;
        },

        getQueryParameterByName: function(name, url) {
            if (!url) {
                url = window.location.href;
            }

            name = name.replace(/[\[\]]/g, '\\$&');
            var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
                results = regex.exec(url);

            if (!results) {
                return null;
            }

            if (!results[2]) {
                return '';
            }

            return decodeURIComponent(results[2].replace(/\+/g, ' '));
        },

        getPath: function(url){
            if (!url) {
                url = window.location.href;
            }

            var response = '';
            var urlParts = url.split('/');

            for (var i = 1; i < urlParts.length; i++) {
                response += '/';
                response += urlParts[i];
            }
            return response;
        }
    };
});
