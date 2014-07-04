define(['omniture'], function () {

    function init() {
        var pageTitle = document.getElementsByTagName('title')[0].textContent;
        var s = s_gi('guardiangudev-code');
        var s_code;

        s.pageName = pageTitle;
        s.channel = 'Membership';
        s_code = s.t();

        if (s_code) {
            document.write(s_code);
        }
    }

    return {
        init: init
    };
});