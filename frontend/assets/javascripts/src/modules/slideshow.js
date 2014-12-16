define(function() {

    var SLIDESHOW_CONTAINER = '.js-slideshow',
        SLIDESHOW_CHILDREN = '.js-slideshow-item',
        CURRENT_CLASS = 'is-current';

    function setCurrentItem( items, currentItem ) {
        [].forEach.call(items, function(item) {
            item.classList.remove(CURRENT_CLASS);
        }, false);
        currentItem.classList.add(CURRENT_CLASS);
    }

    function cycleItems(items, speed) {
        var currentIndex = 0;
        setInterval(function() {
            ++currentIndex;
            currentIndex = (currentIndex >= items.length) ? 0 : currentIndex;
            setCurrentItem(items, items[currentIndex]);
        }, speed);
    }

    function init() {
        var slideshow = document.querySelectorAll(SLIDESHOW_CONTAINER);
        if (slideshow.length) {
            [].forEach.call(slideshow, function(el) {
                var items = el.querySelectorAll(SLIDESHOW_CHILDREN);
                setCurrentItem(items, items[0]);
                cycleItems(items, el.getAttribute('data-slideshow-duration') || 5000);
            }, false);
        }
    }

    return {
        init: init,
        setCurrentItem: setCurrentItem
    };

});
