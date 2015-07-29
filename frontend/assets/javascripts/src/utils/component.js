define(['bean', 'qwery'], function(bean, qwery) {
    'use strict';

    var Component = function() {};

    Component.prototype.elem = document;

    /** @type {Object.<string.Element>} */
    Component.prototype.elems = {};

    /** @type {Object.<string.string>} */
    Component.prototype.classes = null;

    /**
     * @param {string} elemName this corresponds to this.classes
     */
    Component.prototype.getElem = function(elemName) {
        if (this.elems[elemName]) { return this.elems[elemName]; }

        var elem = qwery(this.getClass(elemName), this.elem);
        var element;

        if (elem.length) {
            element = (elem.length === 1) ? elem[0] : elem;
            this.elems[elemName] = element;
        }

        return element;
    };

    /**
     * @param {string} eventName
     * @param {boolean} sansDot
     * @return {string}
     */
    Component.prototype.getClass = function(elemName, sansDot) {
        var className = this.classes[elemName];

        return (sansDot ? '' : '.') + className;
    };

    /**
     * @param {Function} child
     */
    Component.define = function(child) {
        function Tmp() {}
        Tmp.prototype = Component.prototype;
        child.prototype = new Tmp();
        child.prototype.constructor = child;
    };

    return Component;

});
