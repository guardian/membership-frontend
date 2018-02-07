define(['$'], function ($) {
    'use strict';

    var makeAbsolute = function () {
        throw new Error('AJAX has not been initialised yet');
    };

    function ajax(params) {
        if (!params.url.match('^https?://')) {
            params.url = makeAbsolute(params.url);
        }

        var jqParams = {
            url: params.url
        };

        if (params.method) {
            jqParams.type = params.method;
        }

        if (params.data) {
            jqParams.data = params.data;
        }

        if (params.type) {
            jqParams.dataType = params.type;
        }

        if (params.withCredentials) {
            jqParams.xhrFields = {
                withCredentials: params.withCredentials
            };
        }

        return $.ajax(jqParams)
            .done(function(data) {
                if (params.success) {
                    params.success(data);
                }
            })
            .fail(function(jqXHR, textStatus, errorThrown){
                if (params.error) {
                    params.error(errorThrown);
                }
            });
     }

    ajax.init = function (config) {
        makeAbsolute = function (url) {
            return config.page.ajaxUrl + url;
        };
    };

    return ajax;

});
