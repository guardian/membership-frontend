define(['src/modules/form/helper/serializer'], function (serializer) {

    describe('Serializer test', function () {

        it('returns empty object', function () {
            var serializedData = serializer([], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(0);
        });

        it('does not serialize elements with no name attribute', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Lady Miyako';
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(0);
        });

        it('serialize elements with name and type attribute', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Tetsuo Shima';
            elem.name = 'character';
            elem.type = 'text';
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(1);
            expect(serializedData.character).toEqual('Tetsuo Shima');
        });

        it('does not serialize unchecked checkbox elements', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Ryu';
            elem.name = 'character';
            elem.type = 'checkbox';
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(0);
        });

        it('serialize checked checkbox elements', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Colonel Shikishima';
            elem.name = 'character';
            elem.type = 'checkbox';
            elem.checked = true;
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(1);
            expect(serializedData.character).toEqual('Colonel Shikishima');
        });

        it('does not serialize unchecked radio elements', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Kaisuke';
            elem.name = 'character';
            elem.type = 'radio';
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(0);
        });

        it('serialize checked radio elements', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Takashi';
            elem.name = 'character';
            elem.type = 'radio';
            elem.checked = true;
            serializedData = serializer([elem], {});

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(1);
            expect(serializedData.character).toEqual('Takashi');
        });

        it('mixin correctly applied', function () {
            var elem = document.createElement('input');
            var serializedData;

            elem.value = 'Kiyoko';
            elem.name = 'character';
            elem.type = 'text';
            serializedData = serializer([elem], {
                film: 'Akira'
            });

            expect(Object.prototype.toString.call(serializedData)).toBe('[object Object]');
            expect(Object.keys(serializedData).length).toEqual(2);
            expect(serializedData.character).toEqual('Kiyoko');
            expect(serializedData.film).toEqual('Akira');
        });
    });
});
