define(function () {

    /**
     * get parent by className
     * @param $element
     * @param parentClass
     * @returns {*}
     */
    var getSpecifiedParent = function ($element, parentClass) {

        do {
            $element = $element.parent();

        } while ($element && !$element.hasClass(parentClass));

        return $element;
    };

    return {
        getSpecifiedParent: getSpecifiedParent
    };
});
