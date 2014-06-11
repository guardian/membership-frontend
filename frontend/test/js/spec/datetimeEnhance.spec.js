define([
    '$',
    'src/modules/events/DatetimeEnhance'
], function ($, DatetimeEnhance) {

    describe('date time enhance module', function () {

        var now,
            tomorrow,
            dateEnhance,
            twoDaysInTheFuture,
            threeDaysInTheFuture;

        beforeEach(function () {
            dateEnhance = new DatetimeEnhance();

            dateEnhance.saleEndTextElement = document.createElement('div');
            dateEnhance.saleEndTextElement.textContent = 'Sale ends in';

            dateEnhance.saleEndTimeElement = document.createElement('div');
            dateEnhance.saleEndTimeElement.textContent = '17th June 2014, 9:00am';

            now = new Date();

            tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);

            twoDaysInTheFuture = new Date();
            twoDaysInTheFuture.setDate(twoDaysInTheFuture.getDate() + 2);

            threeDaysInTheFuture = new Date();
            threeDaysInTheFuture.setDate(threeDaysInTheFuture.getDate() + 3);
        });

        it('date should display tomorrow string', function () {

            var timeDifference = dateEnhance.calculateTimeDifference(tomorrow.toISOString()),
                timeString = dateEnhance.createEnhancedTimeString(timeDifference);

            expect(timeString.timeLeft).toContain('Tomorrow');
        });

        it('date should display today string', function () {
            var timeDifference = dateEnhance.calculateTimeDifference(now.toISOString()),
                timeString = dateEnhance.createEnhancedTimeString(timeDifference);

            expect(timeString.timeLeft).toContain('Today');
        });

        it('date should display days string', function () {
            var timeDifference = dateEnhance.calculateTimeDifference(threeDaysInTheFuture.toISOString()),
                timeString = dateEnhance.createEnhancedTimeString(timeDifference);

            expect(timeString.timeLeft).toContain('days');
        });

        it('date should display day string', function () {
            var timeDifference = dateEnhance.calculateTimeDifference(twoDaysInTheFuture.toISOString()),
                timeString = dateEnhance.createEnhancedTimeString(timeDifference);

            expect(timeString.timeLeft).toContain('day');
        });

        it('date returns true for tomorrow', function () {
            expect(dateEnhance.isTomorrow(tomorrow)).toBe(true);
        });

        it('date returns true for today', function () {
            expect(dateEnhance.isToday(now)).toBe(true);
        });

        it('dates are equal', function () {
            expect(dateEnhance.datesAreEqual(now, now)).toBe(true);
        });
    });
});

