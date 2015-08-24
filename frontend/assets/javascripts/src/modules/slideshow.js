define(function() {
    'use strict';

    var SLIDESHOW_CONTAINER = '.js-slideshow';
    var SLIDESHOW_CHILDREN = '.js-slideshow-item';
    var SLIDESHOW_POSITION = '.js-slideshow-position';
    var SLIDESHOW_TOTAL = '.js-slideshow-total';
    var SLIDESHOW_NEXT = '.js-slideshow-next';
    var SLIDESHOW_PREV = '.js-slideshow-prev';
    var CURRENT_CLASS = 'is-current';
    var CURRENT_ATTR = 'data-slideshow-current';
    var AUTOPLAY_ATTR = 'data-slideshow-autoplay';
    var AUTOPLAY_DURATION_ATTR = 'data-slideshow-duration';

    function getCurrentIndex(slideshow) {
        return parseInt(slideshow.getAttribute(CURRENT_ATTR), 10) || 0;
    }

    function setCurrentIndex(slideshow, index) {
        slideshow.setAttribute(CURRENT_ATTR, index);
    }

    function setCurrentItem(slideshow, items, index) {
        setCurrentIndex(slideshow, index);
        [].forEach.call(items, function(item) {
            item.classList.remove(CURRENT_CLASS);
        }, false);
        items[index].classList.add(CURRENT_CLASS);
        updateProgress(slideshow, items, index);
    }

    function cycleItems(slideshow, items) {
        setInterval(function() {
            var currentIndex = getCurrentIndex(slideshow);
            var maxIndex = items.length - 1;
            var nextIndex = (currentIndex >= maxIndex) ? 0 : currentIndex + 1;
            setCurrentItem(slideshow, items, nextIndex);
        }, slideshow.getAttribute(AUTOPLAY_DURATION_ATTR) || 6000);
    }

    function updateProgress(slideshow, items, index) {
        var positionEl = slideshow.querySelector(SLIDESHOW_POSITION);
        var totalEl = slideshow.querySelector(SLIDESHOW_TOTAL);

        positionEl.innerHTML = index + 1;
        totalEl.innerHTML = items.length;
    }

    function controlBehaviour(controlEl, slideshow, items, controlFn) {
        if(!controlEl) { return; }
        var maxIndex = items.length - 1;

        controlEl.addEventListener('click', function(e) {
            e.preventDefault();
            var currentIndex = getCurrentIndex(slideshow);
            var nextIndex = controlFn.call(null, currentIndex, maxIndex);
            setCurrentItem(slideshow, items, nextIndex);
        });
    }

    function controlItems(slideshow, items) {
        var next = slideshow.querySelector(SLIDESHOW_NEXT);
        var prev = slideshow.querySelector(SLIDESHOW_PREV);

        controlBehaviour(next, slideshow, items, function(currentIndex, maxIndex) {
            return (currentIndex < maxIndex) ? currentIndex + 1 : 0;
        });
        controlBehaviour(prev, slideshow, items, function(currentIndex, maxIndex) {
            return (currentIndex > 0) ? currentIndex - 1 : maxIndex;
        });
    }

    function init() {
        var slideshows = document.querySelectorAll(SLIDESHOW_CONTAINER);
        if (!slideshows.length) { return; }
        [].forEach.call(slideshows, function(slideshow) {
            var items = slideshow.querySelectorAll(SLIDESHOW_CHILDREN);
            if(!items.length) { return; }
            setCurrentItem(slideshow, items, 0);
            if(slideshow.getAttribute(AUTOPLAY_ATTR) || false) {
                cycleItems(slideshow, items);
            } else {
                controlItems(slideshow, items);
            }
        }, false);
    }

    return {
        init: init
    };

});
