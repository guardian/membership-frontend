define(['$', 'ajax', 'src/utils/user'], function ($, ajax, userUtil) {
    'use strict';

    var SELECTOR = '.js-remaining-tickets';
    var ACTIVE_CLASS = 'is-active';

    function getTicketsRemaining(elem, textElem) {
        ajax({
            url: '/subscription/remaining-tickets',
            type: 'json'
        }).then(function(data) {

            var total = data.totalAllocation;
            var remaining = data.remainingAllocation;

            if (remaining > 0) {
                textElem.html([
                    '<strong>' + remaining + '</strong>',
                    'of',
                    '<strong>' + total + '</strong>',
                    'allocated member tickets remaining.',
                    'You can use these for any Guardian Live event.'
                ].join(' '));
                elem.addClass(ACTIVE_CLASS);
            }
        });
    }

    function init() {
        var elem = $(SELECTOR);
        var textElem = $('.js-remaining-tickets__text');
        if(!elem.length) {
            return;
        }
        userUtil.getMemberDetail(function (memberDetail, hasTier) {
            if (hasTier) {
                getTicketsRemaining(elem, textElem);
            }
        });
    }

    return {
        init: init
    };

});
