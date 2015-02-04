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

    var toArray = function (nodeList) {
        return Array.prototype.slice.call(nodeList);
    };

    var extend = function (target, source) {
        for (var prop in source) {
            if (source.hasOwnProperty(prop)) {
                target[prop] = source[prop];
            }
        }
    };

    return {
        extend: extend,
        toArray: toArray,
        getLocationDetail: getLocationDetail,
        getSpecifiedParent: getSpecifiedParent,
        getOuterHeight: getOuterHeight
    };
});
