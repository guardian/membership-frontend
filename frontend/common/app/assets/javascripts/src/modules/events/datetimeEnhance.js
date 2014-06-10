define(function () {

    return (function () {

        function DateTimeEnhance () {}

        DateTimeEnhance.prototype.config = {
            attribute: {
                SALES_END: 'datetime'
            },
            string: {
                SALE_ENDS_TODAY: 'Sale ends Today',
                SALE_ENDS_TOMORROW: 'Sale ends Tomorrow',
                SALE_ENDS_IN: 'Sale ends in',
                DAY: 'day',
                PLURAL: 's',
                SPACE: ' ',
                OPEN_BRACKET: '(',
                CLOSED_BRACKET: ')'
            }
        };

        /**
         *
         * @param timeLeftOj
         * @returns {*}
         */
        DateTimeEnhance.prototype.createEnhancedTimeString = function (timeLeft) {

            var timeLeftString,
                saleEndString = this.saleEndTimeElement.innerHTML,
                config = this.config;

            if (timeLeft.isToday) {
                timeLeftString = config.string.SALE_ENDS_TODAY;
                saleEndString = saleEndString.split(',')[1].replace(/\s/g, '');
            } else if (timeLeft.isTomorrow) {
                timeLeftString = config.string.SALE_ENDS_TOMORROW;
                saleEndString = saleEndString.split(',')[1].replace(/\s/g, '');
            } else if (timeLeft.days > 0) {
                timeLeftString = [
                        config.string.SALE_ENDS_IN,
                        config.string.SPACE, timeLeft.days,
                        config.string.SPACE, config.string.DAY,
                        (timeLeft.days > 1 ? config.string.PLURAL : '')
                ].join('');
            }

            if (timeLeftString) {
                this.saleEndTextElement.innerHTML = timeLeftString;
                this.saleEndTimeElement.innerHTML = config.string.OPEN_BRACKET + saleEndString + config.string.CLOSED_BRACKET;
            }
        };

        /**
         *
         * @param timestamp
         * @returns {Date}
         */
        DateTimeEnhance.prototype.createDateFromTimestamp = function (timestamp) {
            /* The Eventbrite api appears to be sending a zulu time "2014-06-10T17:30:00.000Z" which is displayed on
            their site as a BST time for sale end. We are not treating this time as a Zulu time we are treating it as BST
            this will need refactoring if eventbrite correct their api */
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
        DateTimeEnhance.prototype.isToday = function (dateToCompare) {
            var now = new Date();

            return this.datesAreEqual(dateToCompare, now);
        };

        /**
         *
         * @param dateToCompare
         * @returns {boolean}
         */
        DateTimeEnhance.prototype.isTomorrow = function (dateToCompare) {
            var now = new Date(),
                nowDayOfMonth = now.getDate();

             now.setDate(nowDayOfMonth + 1);

            return this.datesAreEqual(dateToCompare, now);
        };

        /**
         * compare two dates
         * @param dateOne
         * @param dateTwo
         * @returns {boolean}
         */
        DateTimeEnhance.prototype.datesAreEqual = function (dateOne, dateTwo) {
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
        DateTimeEnhance.prototype.calculateTimeLeft = function (timestamp) {
            var dateFromTimestamp = this.createDateFromTimestamp(timestamp),
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
                isToday: this.isToday(dateFromTimestamp),
                isTomorrow: this.isTomorrow(dateFromTimestamp)
            };
        };

        /**
         * Take timestamp and the pretty scala date from template and calculate how long it is until this date and
         * return the following strings:
         * Sale ends Today (5:30pm)
         * Sale ends Tomorrow (5:30pm)
         * Sale ends in n day(s) (9th June 2014, 5:30pm)
         */
        DateTimeEnhance.prototype.init = function () {

            this.saleEndTextElement = document.querySelectorAll('.js-datetime-enhance-note')[0];
            this.saleEndTimeElement = document.querySelectorAll('.js-datetime-enhance-time')[0];

            if (this.saleEndTextElement) {

                var timestamp = this.saleEndTimeElement.getAttribute('datetime'),
                    timeLeft = this.calculateTimeLeft(timestamp);

                this.createEnhancedTimeString(timeLeft);
            }
        };

        return DateTimeEnhance;

    })();
});
