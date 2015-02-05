define(function () {

    /**
     * get parent by className
     * @param element
     * @param parentClass
     * @returns element
     */
    var getSpecifiedParent = function (element, parentClass) {

        var i = 0;

        do {
            element = element.parentNode;

            if (i > 10) {
                throw 'You are either traversing a lot of elements! Is this wise? Or your $element argument is undefined';
            }
            i++;

        } while (element && !element.classList.contains(parentClass));

        return element;
    };


    var getLocationDetail = function () {
        var windowLocation = window.location;
        return windowLocation.pathname + windowLocation.search;
    };

    /**
     * Get outer height including margin
     * @param  {DOMElement} el
     * @return {String}     outer height including margin
     */
    var getOuterHeight = function(el) {
        var height = el.offsetHeight;
        var style = getComputedStyle(el);

        height += parseInt(style.marginTop, 10) + parseInt(style.marginBottom, 10);
        return height;
    };

    // TODO: Replace with lodash-amd
    var toArray = function (nodeList) {
        return Array.prototype.slice.call(nodeList);
    };

    return {
        toArray: toArray,
        getLocationDetail: getLocationDetail,
        getSpecifiedParent: getSpecifiedParent,
        getOuterHeight: getOuterHeight
    };
});
