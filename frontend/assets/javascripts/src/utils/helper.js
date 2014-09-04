define(function () {

    /**
     * get parent by className
     * @param $element
     * @param parentClass
     * @returns {*}
     */
    var getSpecifiedParent = function ($element, parentClass) {

        var i = 0;

        do {
            $element = $element.parent();

            if (i > 10) {
                throw 'You are either traversing a lot of elements! Is this wise? Or your $element argument is undefined';
            }
            i++;

        } while ($element && !$element.hasClass(parentClass));

        return $element;
    };


    var getLocationDetail = function () {
        var windowLocation = window.location;
        return windowLocation.pathname + windowLocation.search;
    };

    return {
        getLocationDetail: getLocationDetail,
        getSpecifiedParent: getSpecifiedParent
    };
});
