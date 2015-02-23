var tools = tools || {};
tools.commentBuilder = (function(doc) {

    var COMMENT_START = '<!--';
    var COMMENT_END = '-->';
    var provider = doc.querySelector('.js-event-provider');
    var image = doc.querySelector('.js-event-image');
    var tagPickers = doc.querySelectorAll('.js-tag-picker');

    var notSoldThroughEventbrite = doc.querySelector('.js-not-sold-through-eventbrite');
    var resultProvider = doc.querySelector('.js-result-provider');
    var resultImage = doc.querySelector('.js-result-image');
    var resultNotSoldThroughEventbrite = doc.querySelector('.js-result-not-sold-through-eventbrite');
    var resultTagPicker = doc.querySelector('.js-result-custom-tags');

    var createExampleComment = function (value, tag) {
        return [COMMENT_START, ' ', tag, ': ', value, ' ', COMMENT_END].join('');
    };

    var addCommentBuilderListeners = function () {
        image.addEventListener('keyup', function (e) {
            var input = e.target;

            resultProvider.textContent = input.value ? createExampleComment(input.value, input.getAttribute('data-tag')) : '';
        }, false);

        provider.addEventListener('change', function (e) {
            var select = e.target;
            var value = select.options[select.selectedIndex].value;
            var tag = select.getAttribute('data-tag');

            resultImage.textContent = value ? createExampleComment(value, tag) : '';
        }, false);

        notSoldThroughEventbrite.addEventListener('change', function (e) {
            resultNotSoldThroughEventbrite.textContent = e.target.checked ? '<!-- noTicketEvent -->' : '';
        }, false);
    };

    var addTagPickerListeners = function () {
        [].slice.call(tagPickers).forEach(function(elm){
            elm.addEventListener('change', function() {
                var selectedTags = doc.querySelectorAll('.js-tag-picker:checked');
                var tags = [].slice.call(selectedTags).map(function(elm) {
                    return elm.getAttribute('data-tag-name');
                });
                if (tags.length) {
                    tags = COMMENT_START + ' tags:' + tags.join(',') + ' ' + COMMENT_END;
                } else {
                    tags = '';
                }
                resultTagPicker.textContent = tags;
            });
        });
    };

    var init = function () {
        addCommentBuilderListeners();
        addTagPickerListeners();
    };

    return {
        init: init
    };

}(document));
