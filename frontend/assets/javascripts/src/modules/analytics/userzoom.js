/*global _uzactions:true */
define(['src/utils/user'], function (user) {

    var userzoomID = 'D32DB4D34334418399C4C7B3626B05B1';

    function load() {
        user.getMemberDetail(function (memberDetail) {
            if (!user.hasTier(memberDetail)) {
                require(['js!//cdn3.userzoom.com/uz.js?cuid=' + userzoomID]).then(setupTag);
            }
        });
    }

    function setupTag() {
        _uzactions = window._uzactions || [];
        _uzactions.push(['_setID', 'C783AA47ECCEE411B2860022196C4538']);
        _uzactions.push(['_setSID', 'C683AA47ECCEE411B2860022196C4538']);
        _uzactions.push(['_start']);

        require(['js!//cdn4.userzoom.com/trueintent/js/uz_til.js?cuid=' + userzoomID]);
    }

    return {
        load: load
    };
});
