/*global Raven */
define(function() {

    var KRUX_ID = 'JglooLwn';

    function load() {
        require(['js!//cdn.krxd.net/controltag?confid=' + KRUX_ID]).then(null, function(err) {
            Raven.captureException(err);
            Raven.captureMessage('Krux failed to load');
        });
    }

    return {
        load: load
    };
});
