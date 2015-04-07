define(function() {

    var SLIDESHOW_CONTAINER = '.js-slideshow',
        SLIDESHOW_CHILDREN = '.js-slideshow-item',
        SLIDESHOW_POSITION = '.js-slideshow-position',
        SLIDESHOW_TOTAL = '.js-slideshow-total',
        CURRENT_CLASS = 'is-current';

    function setCurrentItem( items, index) {
        [].forEach.call(items, function(item) {
            item.classList.remove(CURRENT_CLASS);
        }, false);
        items[index].classList.add(CURRENT_CLASS);
        updateProgress(items, index);
    }

    function cycleItems(items, speed) {
        var currentIndex = 0;
        setInterval(function() {
            ++currentIndex;
            currentIndex = (currentIndex >= items.length) ? 0 : currentIndex;
            setCurrentItem(items, currentIndex);
        }, speed);
    }

    function updateProgress(items, index) {
        var item = items[index],
            positionEl = item.querySelector(SLIDESHOW_POSITION),
            totalEl = item.querySelector(SLIDESHOW_TOTAL);
        positionEl.innerHTML = index + 1;
        totalEl.innerHTML = items.length;
    }

    function init() {
        var slideshows = document.querySelectorAll(SLIDESHOW_CONTAINER);
        if (slideshows.length) {
            [].forEach.call(slideshows, function(el) {
                var items = el.querySelectorAll(SLIDESHOW_CHILDREN);
                if(items.length) {
                    setCurrentItem(items, 0);
                    cycleItems(items, el.getAttribute('data-slideshow-duration') || 5000);
                }
            }, false);
        }
    }

    return {
        init: init
    };

});
