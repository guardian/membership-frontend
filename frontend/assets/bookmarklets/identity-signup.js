(function() {
    if (!document.getElementById('publicFields_username')) {
        alert('Element with id publicFields_username not found. Are you on the right page?');
        return;
    }
    var username = prompt('Test User key (first name, last name, username and password will be set to this value):');
    if (!username) return;
    ['firstName', 'secondName', 'publicFields_username', 'password', 'primaryEmailAddress'].forEach(function(field) {
        document.getElementById('user_'+field).value = field === 'primaryEmailAddress' ? username + '@gu.com' : username;
    });
    document.querySelector('.js-register-form').submit();
})();
