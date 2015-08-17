define([
    'bonzo',
    'qwery'
], function(bonzo, qwery){
    'use strict';

    function $(selector, context){
        return bonzo(qwery(selector, context));
    }

    $.create = function(s){
        return bonzo(bonzo.create(s));
    };

    return $;
});
