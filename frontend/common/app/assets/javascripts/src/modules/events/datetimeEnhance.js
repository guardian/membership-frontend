define(function () {

    return (function () {

        // TODO
        // put various strings into constants
        // create config object
        // split into a class
        // write tests
        // write comment about what is going on with eventbrite api and times


        var saleEndTextElement,
            saleEndTimeElement;

        /**
         *
         * @param timeLeftOj
         * @returns {*}
         */
        var createTimeString = function (timeLeftOj) {

            var timeLeftString,
                saleEndString = saleEndTimeElement.innerHTML;

            if (timeLeftOj.isToday) {
                timeLeftString = 'Sale ends Today';
                saleEndString = saleEndString.split(',')[1].replace(/\s/g, '');
            } else if (timeLeftOj.isTomorrow) {
                timeLeftString = 'Sale ends Tomorrow';
                saleEndString = saleEndString.split(',')[1].replace(/\s/g, '');
            } else if (timeLeftOj.days > 0) {
                timeLeftString = 'Sale ends in ' + timeLeftOj.days + ' day' + (timeLeftOj.days > 1 ? 's' : '');
            }

            if (timeLeftString) {
                saleEndTextElement.innerHTML = timeLeftString;
                saleEndTimeElement.innerHTML = '(' + saleEndString + ')';
            }
        };

        /**
         *
         * @param timestamp
         * @returns {Date}
         */
        var createDateFromTimestamp = function (timestamp) {

            /*
            The Eventbrite api appears to be sending a zulu time "2014-06-10T17:30:00.000Z" which is displayed on
            their site as a BST time for sale end. We are not treating this time as a Zulu time we are treating it as BST
            this will need refactoring if eventbrite correct their api
             */
            var dateTimeArray = timestamp.slice(0, -1).split('T'),
                dateArray = dateTimeArray[0].split('-'),
                timeArray = dateTimeArray[1].split(':'),
                dateFromTimestamp = new Date(
                    dateArray[0],
                    parseInt(dateArray[1] - 1, 10),
                    dateArray[2],
                    timeArray[0],
                    timeArray[1],
                    timeArray[2]
                );

            return dateFromTimestamp;
        };

        /**
         *
         * @param dateToCompare
         * @returns {boolean}
         */
        var isToday = function (dateToCompare) {
            var now = new Date();

            return datesAreEqual(dateToCompare, now);
        };

        /**
         *
         * @param dateToCompare
         * @returns {boolean}
         */
        var isTomorrow = function (dateToCompare) {
            var now = new Date(),
                nowDayOfMonth = now.getDate();

             now.setDate(nowDayOfMonth + 1);

            return datesAreEqual(dateToCompare, now);
        };

        /**
         * compare two dates
         * @param dateOne
         * @param dateTwo
         * @returns {boolean}
         */
        var datesAreEqual = function (dateOne, dateTwo) {
            if (dateOne.getFullYear() === dateTwo.getFullYear() &&
                dateOne.getDate() === dateTwo.getDate() &&
                dateOne.getMonth() === dateTwo.getMonth()) {

                return true;
            }
        };

        /**
         *
         * @param timestamp
         * @returns {{days: number, hours: number, minutes: number, seconds: number, isToday: boolean, isTomorrow: boolean}}
         */
        var calculateTimeLeft = function (timestamp) {
            var dateFromTimestamp = createDateFromTimestamp(timestamp),
                now = new Date(),
                milliSecondDifference = dateFromTimestamp - now.getTime(),
                secondDifference,
                minuteDifference,
                hourDifference,
                seconds,
                minutes,
                hours,
                days;

            //convert milliseconds to seconds
            secondDifference = milliSecondDifference/1000;
            seconds = Math.floor(secondDifference % 60);

            //convert seconds to minutes
            minuteDifference = secondDifference / 60;
            minutes = Math.floor(minuteDifference % 60);

            //convert minutes to hours
            hourDifference = minuteDifference / 60;
            hours = Math.floor(hourDifference % 24);

            //divide hours into days
            days = Math.floor(hourDifference / 24);

            return {
                days: days,
                hours: hours,
                minutes: minutes,
                seconds: seconds,
                isToday: isToday(dateFromTimestamp),
                isTomorrow: isTomorrow(dateFromTimestamp)
            };
        };

        /**
         * Take timestamp and the pretty scala date from template and calculate how long it is until this date and
         * return the following strings:
         * Sale ends Today (5:30pm)
         * Sale ends Tomorrow (5:30pm)
         * Sale ends in n day(s) (9th June 2014, 5:30pm)
         */
        var init = function () {

            saleEndTextElement = document.querySelectorAll('.js-datetime-enhance-note')[0];
            saleEndTimeElement = document.querySelectorAll('.js-datetime-enhance-time')[0];

            //var timestamp = saleEndTimeElement.getAttribute('datetime');
            var timestamp = '2014-06-10T17:30:00.000Z',
                timeLeftObj = calculateTimeLeft(timestamp);

            createTimeString(timeLeftObj);
        };

        return {
            init: init
        };

    })();
});
