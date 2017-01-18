define([], function() {
    return{
        init: function(){
            var selected = document.getElementById('c_' + window.location.hash.substr(1))
            if(selected && selected.type == 'checkbox'){
                selected.checked = true;
            }
        }
    }
});
