define(function () {

   return (function () {

       var enhancers = {};
       var self = this;


       self.eventbriteDateDiff = function (d) {

           var _MS_PER_DAY = 1000 * 60 * 60 * 24;
           var _MS_PER_HOUR = 1000 * 60 * 60;
           var _MS_PER_MIN = 1000 * 60;

           var now = new Date();

           // Discard the time and time-zone information.
           var now_utc = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), now.getMinutes());
           var utc = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), d.getUTCHours(), d.getUTCMinutes(), d.getUTCSeconds());

           var diff = utc - now_utc;

           var days = Math.floor(diff / _MS_PER_DAY);
           var hours = Math.floor((diff - (days*_MS_PER_DAY)) / _MS_PER_HOUR);
           var mins = Math.floor((diff - (days*_MS_PER_DAY) - (hours*_MS_PER_HOUR)) / _MS_PER_MIN);

           return {
               millis_total: diff,
               days: days,
               hours: hours,
               mins: mins
           };
       };

       self.dateTimeEnhancer = function (element) {

           var time_el = element.querySelector('.js-datetime-enhance-time');
           var note_el = element.querySelector('.js-datetime-enhance-note');

           var utc_timestamp_date = new Date(time_el.getAttribute('datetime'));

           var diff = self.eventbriteDateDiff(utc_timestamp_date);

           var diff_string = [];

           diff_string.push(diff.days);
           diff_string.push('d ');
           diff_string.push(diff.hours);
           diff_string.push('h ');
           diff_string.push(diff.mins);
           diff_string.push('m');

           time_el.innerHTML = diff_string.join('');
           note_el.innerHTML = note_el.innerHTML.replace('at', 'in');
       };

       return {
           init: function (context) {
               context = context || document;
               var dateTimes = context.querySelectorAll('.js-datetime-enhance');

               if (dateTimes) {
                   for (var i = 0; i < dateTimes.length; i++) {
                       dateTimes[i].setAttribute('data-datetimeEnhancer', i);
                       enhancers[i] = new self.dateTimeEnhancer(dateTimes[i]);
                   }
               }
           }
       };

   })();
});
