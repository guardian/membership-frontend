define([
    '$',
    'src/modules/events/datetimeEnhance'
], function ($, datetimeEnhance) {

    describe('Events module', function() {

        /********************************************************
         * DateTime enhancer - datetimeEnhance.js
         ********************************************************/

/*        it('should correctly enhance a date to a countdown', function() {

            var now = new Date();

            var hours = now.getHours();

            var now_plus_one_hour = new Date(new Date().setHours(hours + 2));

            var mockDatetimeHtml = [];
            mockDatetimeHtml.push('<div>');
            mockDatetimeHtml.push('<div class="js-datetime-enhance">');
            mockDatetimeHtml.push('<span class="event__sale_ends_note js-datetime-enhance-note">Sales end at </span>');
            mockDatetimeHtml.push('<span class="event__sale_ends_time js-datetime-enhance-time" datetime="');
            mockDatetimeHtml.push(now_plus_one_hour.toISOString());
            mockDatetimeHtml.push('"></span></div></div>');

            mockDatetimeElement = $.create(mockDatetimeHtml.join(''));

            datetimeEnhance.init(mockDatetimeElement[0]);

            expect(
                mockDatetimeElement[0].querySelector('.js-datetime-enhance-time').innerHTML
            ).toEqual('0d 1h 0m');

        });*/

    });

});

