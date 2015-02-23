var tools = tools || {};
tools.commentBuilder = (function(doc) {

    var COMMENT_START = '<!--';
    var COMMENT_END = '-->';
    var provider = doc.querySelector('.js-event-provider');
    var image = doc.querySelector('.js-event-image');
    var notSoldThroughEventbrite = doc.querySelector('.js-not-sold-through-eventbrite');
    var resultProvider = doc.querySelector('.js-result-provider');
    var resultImage = doc.querySelector('.js-result-image');
    var resultNotSoldThroughEventbrite = doc.querySelector('.js-result-not-sold-through-eventbrite');

    var createExampleComment = function (value, tag) {
        return [COMMENT_START, ' ', tag, ': ', value, ' ', COMMENT_END].join('');
    };

    var addCommentBuilderListeners = function () {
        image.addEventListener('keyup', function (e) {
            var input = e.target;

            resultProvider.classList[input.value ? 'remove' : 'add']('is-hidden');
            resultProvider.textContent = input.value ? createExampleComment(input.value, input.getAttribute('data-tag')) : '';
        }, false);

        provider.addEventListener('change', function (e) {
            var select = e.target;
            var value = select.options[select.selectedIndex].value;
            var tag = select.getAttribute('data-tag');

            resultImage.classList[value ? 'remove' : 'add']('is-hidden');
            resultImage.textContent = value ? createExampleComment(value, tag) : '';
        }, false);

        notSoldThroughEventbrite.addEventListener('change', function (e) {
            var isChecked = e.target.checked;

            resultNotSoldThroughEventbrite.classList[isChecked ? 'remove' : 'add']('is-hidden');
            resultNotSoldThroughEventbrite.textContent = isChecked ? '<!-- noTicketEvent -->' : '';
        }, false);
    };

    var init = function () {
        addCommentBuilderListeners();
    };

    return {
        init: init
    };

}(document));
