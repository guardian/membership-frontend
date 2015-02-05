define(function() {
    return {
        isExternalLink: function(url) {
            var external = url.replace('http://','').replace('https://','').split('/')[0];
            return (external.length) ? true : false;
        }
    };
});
