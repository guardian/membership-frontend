import * as display from 'src/modules/form/validation/display'
import * as helper from 'src/utils/helper'
import ajax from 'ajax'
import $ from '$'



function ajaxRequest(allowP){

    ajax({
        url: 'https://members-data-api.thegulocal.com/user-attributes/tier/public',
        method: 'POST',
        withCredentials: true,
        data: {
            allowPublic: allowP                                                                         
        }
    });
}

export function init() {
    if (!document.querySelector('.memCheck')) {
        return;
    }

    // Custom amount
    document.querySelector('.memCheck').addEventListener('change', function (e) {
        if (e.target.checked) {
            //opt in
            ajaxRequest(true);
        }else {
            //opt out
            ajaxRequest(false);
        }
    });

}
