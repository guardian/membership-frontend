define(['ajax'
], function(ajax) {

    //todo handle last name too
    var check = function(id, postcode) {
        return ajax({
            url: '/user/subscriber/details?id='+ id + '&postcode=' + postcode
        });
    };

    return {
        check: check
    };

});