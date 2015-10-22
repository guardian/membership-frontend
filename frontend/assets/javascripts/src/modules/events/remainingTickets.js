define([
    '$',
    'ajax',
    'src/utils/user'
], function ($, ajax, userUtil) {
    'use strict';

    var ELEM_SELECTOR = '.js-remaining-tickets';
    var ELEM_TEXT_SELECTOR = '.js-remaining-tickets__text';
    var ACTIVE_CLASS = 'is-active';

    function setMessage(props, elem) {

        var textElem = $(ELEM_TEXT_SELECTOR, elem);

        var newUserMessageParts = [
            'You can use one of your',
            '<strong>' + props.total + '</strong>',
            'allocated member tickets',
            ((props.mode === 'event') ? 'for this event.' : 'for any Guardian Live event.')
        ];

        var returningUserMessageParts = [
            'You have',
            '<strong>' + props.remaining + '</strong>',
            'of',
            '<strong>' + props.total + '</strong>',
            'allocated member tickets remaining.',
            ((props.mode === 'event') ? 'You can use one for this event.' : 'You can use these for any Guardian Live event.')
        ];

        var messageParts = (props.remaining === props.total) ? newUserMessageParts : returningUserMessageParts;

        if(textElem.length && messageParts.length) {
            textElem.html(messageParts.join(' '));
            elem.addClass(ACTIVE_CLASS);
        }
    }

    function init() {
        var elem = $(ELEM_SELECTOR);
        if(!elem.length) {
            return;
        }

        userUtil.getMemberDetail(function (memberDetail, hasTier) {
            if (hasTier && memberDetail.benefits && memberDetail.benefits.complimentaryEventTickets) {
                ajax({
                    url: '/subscription/remaining-tickets',
                    type: 'json'
                }).then(function(data) {
                    var props = {
                        mode: elem.data('mode'),
                        total: data.totalAllocation,
                        remaining: data.remainingAllocation
                    };
                    if (props.remaining > 0) {
                        setMessage(props, elem);
                    }
                });
            }
        });
    }

    return {
        init: init
    };

});
